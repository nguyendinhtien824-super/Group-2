package repository;

import model.FlashItem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FlashItemRepository extends CsvRepository<FlashItem> {
    private static final int MAX_RETRY = 3;
    private final Object fileLockMonitor = new Object();

    public FlashItemRepository() {
        super("flash_items.csv", "itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,version");
    }

    public FlashItemRepository(String dataDirectory) {
        super(dataDirectory, "flash_items.csv", "itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,version");
    }

    @Override
    protected FlashItem parseLine(String line) {
        return FlashItem.fromCsvLine(line);
    }

    public boolean sellWithOptimisticLock(String itemId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            FlashItem item = findById(itemId);
            if (item == null || item.getRemainingStock() < quantity) {
                return false;
            }

            int expectedVersion = item.getVersion();
            item.setSoldQty(item.getSoldQty() + quantity);
            item.setVersion(expectedVersion + 1);

            if (update(item, expectedVersion)) {
                return true;
            }
        }

        return false;
    }

    public boolean sellWithFileLock(String itemId, int quantity) {
        synchronized (fileLockMonitor) {
            Path lockPath = Paths.get(filePath + ".lock");
            try {
                Files.createDirectories(lockPath.getParent());
                if (!Files.exists(lockPath)) {
                    Files.createFile(lockPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to prepare file lock: " + lockPath, e);
            }

            try (RandomAccessFile file = new RandomAccessFile(lockPath.toFile(), "rw");
                 FileChannel channel = file.getChannel();
                 FileLock ignored = channel.lock()) {
                return sellWithOptimisticLock(itemId, quantity);
            } catch (IOException e) {
                throw new RuntimeException("Failed to lock flash_items.csv", e);
            }
        }
    }

    public synchronized boolean sellWithSynchronized(String itemId, int quantity) {
        FlashItem item = findById(itemId);
        if (item == null || item.getRemainingStock() < quantity) {
            return false;
        }

        int expectedVersion = item.getVersion();
        item.setSoldQty(item.getSoldQty() + quantity);
        item.setVersion(expectedVersion + 1);
        return update(item, expectedVersion);
    }
}
