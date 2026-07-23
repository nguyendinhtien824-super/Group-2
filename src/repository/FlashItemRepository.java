package repository;

import model.FlashItem;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/** CSV repository exposing the four lock mechanisms required by the simulator. */
public class FlashItemRepository extends CsvRepository<FlashItem> {
    public static final int MAX_RETRY = 3;

    private static final long RACE_WINDOW_NANOS = 5_000_000L;
    private static final String FILE_NAME = "flash_items.csv";
    private static final String HEADER =
            "itemId,productId,eventId,productName,originalPrice,salePrice," +
                    "initialStock,soldQty,remainingStock,version";

    public FlashItemRepository() {
        super(FlashItem.class, FILE_NAME, HEADER);
    }

    public FlashItemRepository(String dataDirectory) {
        super(FlashItem.class, dataDirectory, FILE_NAME, HEADER);
    }

    @Override
    public void save(FlashItem item) {
        FlashItemValidator.validate(item);
        super.save(item);
    }

    @Override
    public void saveAll(List<FlashItem> items) {
        items.forEach(FlashItemValidator::validate);
        super.saveAll(items);
    }

    public boolean updateItem(FlashItem item, int expectedVersion) {
        FlashItemValidator.validate(item);
        item.setVersion(expectedVersion + 1);
        return super.update(item, expectedVersion);
    }

    /** Intentionally performs a stale read/write cycle without any lock. */
    public boolean sellNoLock(String itemId, int quantity) {
        if (!isValidRequest(itemId, quantity)) {
            return false;
        }

        List<FlashItem> items;
        getRwLock().readLock().lock();
        try {
            items = readAllUnlocked();
        } finally {
            getRwLock().readLock().unlock();
        }
        FlashItem item = findItem(items, itemId);
        if (!hasStock(item, quantity)) {
            return false;
        }

        LockSupport.parkNanos(RACE_WINDOW_NANOS);
        applySale(item, quantity);
        getRwLock().writeLock().lock();
        try {
            writeAllUnlocked(items);
        } finally {
            getRwLock().writeLock().unlock();
        }
        return true;
    }

    /** Uses a JVM monitor on the repository and the shared per-path CSV lock. */
    public synchronized boolean sellWithSynchronized(String itemId, int quantity) {
        if (!isValidRequest(itemId, quantity)) {
            return false;
        }

        getRwLock().writeLock().lock();
        try {
            return sellCurrentState(itemId, quantity);
        } finally {
            getRwLock().writeLock().unlock();
        }
    }

    /**
     * Uses a real OS-level FileLock on a stable sidecar file. The sidecar remains
     * lockable while CsvRepository atomically replaces the CSV destination.
     */
    public boolean sellWithFileLock(String itemId, int quantity) {
        if (!isValidRequest(itemId, quantity)) {
            return false;
        }

        Path lockPath = getFilePath().resolveSibling(getFilePath().getFileName() + ".lck");
        getRwLock().writeLock().lock();
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return sellCurrentState(itemId, quantity);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot lock CSV file: " + getFilePath(), exception);
        } finally {
            getRwLock().writeLock().unlock();
        }
    }

    public boolean sellWithOptimisticLock(String itemId, int quantity) {
        return sellWithOptimisticLock(itemId, quantity, MAX_RETRY, new AtomicInteger());
    }

    public boolean sellWithOptimisticLock(
            String itemId, int quantity, AtomicInteger retryCounter) {
        return sellWithOptimisticLock(itemId, quantity, MAX_RETRY, retryCounter);
    }

    /** Compares the persisted version and retries a conflict at most three times. */
    public boolean sellWithOptimisticLock(
            String itemId, int quantity, int maxRetries, AtomicInteger retryCounter) {
        if (!isValidRequest(itemId, quantity)) {
            return false;
        }
        if (retryCounter == null) {
            throw new IllegalArgumentException("retryCounter must not be null");
        }

        int retryLimit = Math.max(0, Math.min(MAX_RETRY, maxRetries));
        for (int attempt = 0; attempt <= retryLimit; attempt++) {
            FlashItem snapshot = findById(itemId);
            if (!hasStock(snapshot, quantity)) {
                return false;
            }

            getRwLock().writeLock().lock();
            try {
                List<FlashItem> items = readAllUnlocked();
                FlashItem current = findItem(items, itemId);
                if (!hasStock(current, quantity)) {
                    return false;
                }
                if (current.getVersion() != snapshot.getVersion()) {
                    if (attempt < retryLimit) {
                        retryCounter.incrementAndGet();
                    }
                    continue;
                }

                applySale(current, quantity);
                writeAllUnlocked(items);
                return true;
            } finally {
                getRwLock().writeLock().unlock();
            }
        }
        return false;
    }

    private boolean sellCurrentState(String itemId, int quantity) {
        List<FlashItem> items = readAllUnlocked();
        FlashItem item = findItem(items, itemId);
        if (!hasStock(item, quantity)) {
            return false;
        }
        applySale(item, quantity);
        writeAllUnlocked(items);
        return true;
    }

    private static FlashItem findItem(List<FlashItem> items, String itemId) {
        return items.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasStock(FlashItem item, int quantity) {
        return item != null && item.getRemainingStock() >= quantity;
    }

    private static boolean isValidRequest(String itemId, int quantity) {
        return itemId != null && !itemId.isBlank() && quantity > 0;
    }

    private static void applySale(FlashItem item, int quantity) {
        item.setSoldQty(item.getSoldQty() + quantity);
        item.setVersion(item.getVersion() + 1);
    }
}
