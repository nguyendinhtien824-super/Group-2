package test;

import model.FlashItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.FlashItemRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LockMechanismRepositoryTest {
    private static final String ITEM_ID = "FI-LOCK-001";

    private Path testDirectory;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("lock-mechanism-test-");
    }

    @After
    public void tearDown() throws IOException {
        if (testDirectory == null || !Files.exists(testDirectory)) {
            return;
        }
        try (var paths = Files.walk(testDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot clean test path: " + path, exception);
                }
            });
        }
    }

    @Test
    public void fileLockUsesSidecarAndNeverOversells() throws Exception {
        Path dataDirectory = testDirectory.resolve("file-lock");
        FlashItemRepository repository = createRepository(dataDirectory, 25);

        int successes = runConcurrentSales(repository::sellWithFileLock, 80);
        FlashItem finalItem = repository.findById(ITEM_ID);

        assertEquals(25, successes);
        assertProtectedState(finalItem, successes);
        assertTrue(Files.exists(dataDirectory.resolve("flash_items.csv.lck")));
    }

    @Test
    public void synchronizedRepositoryNeverOversells() throws Exception {
        FlashItemRepository repository = createRepository(
                testDirectory.resolve("synchronized"), 25);

        int successes = runConcurrentSales(repository::sellWithSynchronized, 80);
        FlashItem finalItem = repository.findById(ITEM_ID);

        assertEquals(25, successes);
        assertProtectedState(finalItem, successes);
    }

    @Test
    public void optimisticVersionCheckRetriesAtMostThreeTimesPerBuyer() throws Exception {
        FlashItemRepository repository = createRepository(
                testDirectory.resolve("optimistic"), 100);
        AtomicInteger retryCount = new AtomicInteger();

        int successes = runConcurrentSales(
                (itemId, quantity) -> repository.sellWithOptimisticLock(
                        itemId, quantity, 99, retryCount),
                80);
        FlashItem finalItem = repository.findById(ITEM_ID);

        assertEquals(3, FlashItemRepository.MAX_RETRY);
        assertTrue(retryCount.get() <= 80 * FlashItemRepository.MAX_RETRY);
        assertProtectedState(finalItem, successes);
        assertEquals(successes, finalItem.getVersion());
    }

    @Test
    public void noLockIntentionallyExposesRaceCondition() throws Exception {
        int initialStock = 2;
        FlashItemRepository repository = createRepository(
                testDirectory.resolve("no-lock"), initialStock);

        int successes = runConcurrentSales(repository::sellNoLock, 120);
        FlashItem finalItem = repository.findById(ITEM_ID);

        assertNotNull(finalItem);
        assertTrue("No-lock must expose logical overselling", successes > initialStock);
        assertTrue("Persisted state must reveal lost updates",
                finalItem.getSoldQty() != successes);
    }

    private FlashItemRepository createRepository(Path directory, int stock) {
        FlashItemRepository repository = new FlashItemRepository(directory.toString());
        repository.save(new FlashItem(
                ITEM_ID, "P-LOCK-001", "EV-LOCK-001", "Lock Test Product",
                100_000, 50_000, stock));
        return repository;
    }

    private int runConcurrentSales(SaleOperation operation, int threadCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successes = new AtomicInteger();
        try {
            for (int index = 0; index < threadCount; index++) {
                executor.execute(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (operation.sell(ITEM_ID, 1)) {
                            successes.incrementAndGet();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(45, TimeUnit.SECONDS));
            return successes.get();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void assertProtectedState(FlashItem finalItem, int successes) {
        assertNotNull(finalItem);
        assertTrue(finalItem.getRemainingStock() >= 0);
        assertEquals(successes, finalItem.getSoldQty());
        assertEquals(finalItem.getInitialStock() - successes, finalItem.getRemainingStock());
    }

    @FunctionalInterface
    private interface SaleOperation {
        boolean sell(String itemId, int quantity);
    }
}

// Member 3
