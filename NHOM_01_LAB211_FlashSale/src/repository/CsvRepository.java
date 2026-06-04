package repository;

import model.BaseEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Generic Repository de doc/ghi file CSV.
 * Yeu cau tu PDF: Implement CsvRepository<T> generic day du.
 */
public abstract class CsvRepository<T extends BaseEntity> {
    protected final String filePath;
    protected final String header;
    protected final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public CsvRepository(String fileName, String header) {
        this("data", fileName, header);
    }

    public CsvRepository(String dataDirectory, String fileName, String header) {
        this.filePath = Paths.get(dataDirectory, fileName).toString();
        this.header = header;
        ensureFileExists();
    }

    private void ensureFileExists() {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, header + System.lineSeparator(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create CSV file: " + filePath, e);
            }
        }
    }

    protected abstract T parseLine(String line);

    public List<T> findAll() {
        rwLock.readLock().lock();
        try {
            List<T> result = new ArrayList<>();
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                T entity = parseLine(line);
                if (entity != null) {
                    result.add(entity);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV: " + filePath, e);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public T findById(String id) {
        return findAll().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean update(T entity, int expectedVersion) {
        rwLock.writeLock().lock();
        try {
            List<T> all = findAll();
            for (int i = 0; i < all.size(); i++) {
                T current = all.get(i);
                if (current.getId().equals(entity.getId())) {
                    // Logic Optimistic Locking
                    if (current instanceof model.FlashItem && ((model.FlashItem) current).getVersion() != expectedVersion) {
                        return false; // Version conflict
                    }
                    all.set(i, entity);
                    writeAll(all);
                    return true;
                }
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void saveAll(List<T> entities) {
        rwLock.writeLock().lock();
        try {
            List<T> all = findAll();
            Map<String, T> map = new LinkedHashMap<>();
            for (T e : all) map.put(e.getId(), e);
            for (T e : entities) map.put(e.getId(), e);
            
            writeAll(new ArrayList<>(map.values()));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void save(T entity) {
        saveAll(Collections.singletonList(entity));
    }

    public boolean deleteById(String id) {
        rwLock.writeLock().lock();
        try {
            List<T> all = findAll();
            boolean removed = all.removeIf(e -> e.getId().equals(id));
            if (removed) {
                writeAll(all);
            }
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void writeAll(List<T> entities) {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            w.write(header);
            w.newLine();
            for (T e : entities) {
                w.write(e.toCsvLine());
                w.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV: " + filePath, e);
        }
    }
}
