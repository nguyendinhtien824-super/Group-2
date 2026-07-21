package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import model.FlashItem;
import repository.FlashItemRepository;

public class ConcurrencyLockTest {

    private static final String TEST_DIR = "test_data";

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        deleteDirectory(new File("tuan 4/" + TEST_DIR));
        deleteDirectory(new File("tuan 3/" + TEST_DIR));
        new File(TEST_DIR).mkdirs();
    }

    @After
    public void tearDown() {
        // deleteDirectory(new File(TEST_DIR));
        // deleteDirectory(new File("tuan 4/" + TEST_DIR));
        // deleteDirectory(new File("tuan 3/" + TEST_DIR));
    }

    @Test
    public void testSellNoLockConcurrency() throws InterruptedException {
        String path = TEST_DIR + "/noLock";

        FlashItemRepository flashRepo = new FlashItemRepository(path);
        String itemId = "FI-00001";
        FlashItem item = new FlashItem(itemId, "P-00001", "EV-001", "No Lock Product", 100000, 50000, 100);
        flashRepo.save(item);

        int numThreads = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successCalls = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean res = flashRepo.sellNoLock(itemId, 1);
                    if (res) {
                        successCalls.incrementAndGet();
                    } else {
                        failedCalls.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        FlashItem finalItem = flashRepo.findById(itemId);
        assertNotNull(finalItem);
        int realRemainingStock = item.getInitialStock() - successCalls.get();
        // Với no-lock, race condition xảy ra khiến lượng bán thành công vượt quá kho ban đầu, dẫn đến tồn kho thực tế bị âm.
        assertTrue("Tồn kho bị âm thực tế do race condition! Số lượng kho còn lại sau khi đã bán: " + realRemainingStock,
                realRemainingStock < 0);
    }

    @Test
    public void testSellWithOptimisticLockConcurrency() throws InterruptedException {
        String path = TEST_DIR + "/optLock";

        FlashItemRepository flashRepo = new FlashItemRepository(path);
        String itemId = "FI-00002";
        FlashItem item = new FlashItem(itemId, "P-00001", "EV-001", "Optimistic Lock Product", 100000, 50000, 1);
        flashRepo.save(item);

        int numThreads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successCalls = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean res = flashRepo.sellWithOptimisticLock(itemId, 1);
                    if (res) {
                        successCalls.incrementAndGet();
                    } else {
                        failedCalls.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        FlashItem finalItem = flashRepo.findById(itemId);
        assertNotNull(finalItem);

        assertEquals("Success calls must match soldQty in DB", successCalls.get(), finalItem.getSoldQty());
        assertEquals("Version must equal soldQty", successCalls.get(), finalItem.getVersion());
        assertTrue("There must be failed calls due to version conflicts", failedCalls.get() > 0);
    }

    @Test
    public void testSellWithFileLockConcurrency() throws InterruptedException {
        String path = TEST_DIR + "/fileLock";

        FlashItemRepository flashRepo = new FlashItemRepository(path);
        String itemId = "FI-00003";
        FlashItem item = new FlashItem(itemId, "P-00001", "EV-001", "File Lock Product", 100000, 50000, 5);
        flashRepo.save(item);

        int numThreads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successCalls = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean res = flashRepo.sellWithFileLock(itemId, 1);
                    if (res) {
                        successCalls.incrementAndGet();
                    } else {
                        failedCalls.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        FlashItem finalItem = flashRepo.findById(itemId);
        assertNotNull(finalItem);

        int initialStock = item.getInitialStock();
        int expectedSuccess = Math.min(initialStock, numThreads);
        int expectedFailed = numThreads - expectedSuccess;
        int expectedRemaining = initialStock - expectedSuccess;

        assertEquals("Số người mua thành công không khớp", expectedSuccess, successCalls.get());
        assertEquals("Số người mua thất bại không khớp", expectedFailed, failedCalls.get());
        assertEquals("soldQty trong DB không khớp", expectedSuccess, finalItem.getSoldQty());
        assertEquals("Tồn kho còn lại không khớp", expectedRemaining, finalItem.getRemainingStock());
    }
}
