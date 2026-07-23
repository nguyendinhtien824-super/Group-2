package repository;

import model.BaseEntity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Generic, UTF-8 CSV repository with per-path locking and atomic replacement. */
public class CsvRepository<T extends BaseEntity> {
    private static final ConcurrentMap<Path, ReentrantReadWriteLock> PATH_LOCKS =
            new ConcurrentHashMap<>();

    private final Path filePath;
    private final String header;
    private final CsvReflectionMapper<T> mapper;
    private final ReentrantReadWriteLock rwLock;

    public CsvRepository(Class<T> type, String fileName, String header) {
        this(type, "data", fileName, header);
    }

    public CsvRepository(Class<T> type, String dataDirectory, String fileName, String header) {
        this.header = requireText(header, "header");
        this.filePath = Path.of(requireText(dataDirectory, "dataDirectory"),
                        requireText(fileName, "fileName"))
                .toAbsolutePath()
                .normalize();
        this.mapper = new CsvReflectionMapper<>(Objects.requireNonNull(type, "type"), header);
        this.rwLock = PATH_LOCKS.computeIfAbsent(filePath, ignored -> new ReentrantReadWriteLock(true));
        ensureFileExists();
    }

    public ReentrantReadWriteLock getRwLock() {
        return rwLock;
    }

    protected final Path getFilePath() {
        return filePath;
    }

    protected final String getHeader() {
        return header;
    }

    public List<T> findAll() {
        rwLock.readLock().lock();
        try {
            return readAllUnlocked();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public T findById(String id) {
        if (id == null) {
            return null;
        }
        return findAll().stream()
                .filter(entity -> id.equals(entity.getId()))
                .findFirst()
                .orElse(null);
    }

    /** Update with version comparison when the entity declares a version field. */
    public boolean update(T entity, int expectedVersion) {
        validateEntity(entity);
        rwLock.writeLock().lock();
        try {
            List<T> all = readAllUnlocked();
            for (int index = 0; index < all.size(); index++) {
                T current = all.get(index);
                if (!entity.getId().equals(current.getId())) {
                    continue;
                }
                Integer currentVersion = mapper.readOptionalVersion(current);
                if (currentVersion != null && currentVersion != expectedVersion) {
                    return false;
                }
                all.set(index, entity);
                writeAllUnlocked(all);
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void saveAll(List<T> entities) {
        Objects.requireNonNull(entities, "entities");
        rwLock.writeLock().lock();
        try {
            Map<String, T> byId = new LinkedHashMap<>();
            for (T current : readAllUnlocked()) {
                byId.put(current.getId(), current);
            }
            for (T entity : entities) {
                validateEntity(entity);
                byId.put(entity.getId(), entity);
            }
            writeAllUnlocked(new ArrayList<>(byId.values()));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void save(T entity) {
        saveAll(Collections.singletonList(entity));
    }

    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        rwLock.writeLock().lock();
        try {
            List<T> all = readAllUnlocked();
            boolean removed = all.removeIf(entity -> id.equals(entity.getId()));
            if (removed) {
                writeAllUnlocked(all);
            }
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Read primitive for subclasses that already hold the correct lock. */
    protected final List<T> readAllUnlocked() {
        List<T> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(mapper.parse(line, filePath.getFileName().toString()));
                }
            }
            return result;
        } catch (IOException exception) {
            throw storageFailure("read", exception);
        }
    }

    /** Atomic write primitive for subclasses that already hold the correct lock. */
    protected final void writeAllUnlocked(List<T> entities) {
        Path staged = null;
        try {
            Files.createDirectories(filePath.getParent());
            staged = Files.createTempFile(filePath.getParent(), filePath.getFileName().toString(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(staged, StandardCharsets.UTF_8)) {
                writer.write(header);
                writer.newLine();
                for (T entity : entities) {
                    writer.write(mapper.serialize(entity));
                    writer.newLine();
                }
            }
            replaceDestination(staged);
        } catch (IOException exception) {
            throw storageFailure("write", exception);
        } finally {
            deleteStagedFile(staged);
        }
    }

    private void ensureFileExists() {
        rwLock.writeLock().lock();
        try {
            if (Files.exists(filePath)) {
                return;
            }
            Files.createDirectories(filePath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(header);
                writer.newLine();
            }
        } catch (IOException exception) {
            throw storageFailure("create", exception);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void replaceDestination(Path staged) throws IOException {
        try {
            Files.move(staged, filePath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(staged, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteStagedFile(Path staged) {
        if (staged == null) {
            return;
        }
        try {
            Files.deleteIfExists(staged);
        } catch (IOException ignored) {
            // The destination is already intact; cleanup is retried by the final hygiene scan.
        }
    }

    private void validateEntity(T entity) {
        Objects.requireNonNull(entity, "entity");
        if (entity.getId() == null || entity.getId().isBlank()) {
            throw new IllegalArgumentException("Entity id must not be blank");
        }
    }

    private IllegalStateException storageFailure(String operation, IOException cause) {
        return new IllegalStateException("Cannot " + operation + " CSV file: " + filePath, cause);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
