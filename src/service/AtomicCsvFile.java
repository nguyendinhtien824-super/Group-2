package service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stages a generated CSV and replaces the destination only after commit. */
final class AtomicCsvFile implements AutoCloseable {
    private final Path destination;
    private final Path staged;
    private final BufferedWriter writer;
    private boolean writerClosed;
    private boolean committed;

    private AtomicCsvFile(Path destination, Path staged, BufferedWriter writer) {
        this.destination = destination;
        this.staged = staged;
        this.writer = writer;
    }

    static AtomicCsvFile open(Path destination) throws IOException {
        Path normalized = destination.toAbsolutePath().normalize();
        Files.createDirectories(normalized.getParent());
        Path staged = Files.createTempFile(
                normalized.getParent(), normalized.getFileName().toString(), ".tmp");
        return new AtomicCsvFile(normalized, staged,
                Files.newBufferedWriter(staged, StandardCharsets.UTF_8));
    }

    BufferedWriter writer() {
        return writer;
    }

    void commit() throws IOException {
        closeWriter();
        try {
            Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        committed = true;
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            closeWriter();
        } catch (IOException exception) {
            failure = exception;
        }
        if (!committed) {
            try {
                Files.deleteIfExists(staged);
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void closeWriter() throws IOException {
        if (!writerClosed) {
            writer.close();
            writerClosed = true;
        }
    }
}
