package service;

import model.FlashItem;
import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import repository.FlashItemRepository;
import repository.OrderTransactionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Orchestrates CSV-backed lock simulations and benchmark matrices. */
public class SimulatorService {
    public static final int QUICK_MIN_THREADS = 100;
    public static final int QUICK_MAX_THREADS = 500;
    public static final int FULL_THREADS = 1000;
    public static final int FULL_REPEATS = 3;
    public static final int MAX_RETRY = FlashItemRepository.MAX_RETRY;
    static final String ITEM_ID = "FI-SIM-001";

    private static final int MAX_THREADS = FULL_THREADS;
    private static final String ITEM_FILE_NAME = "flash_items.csv";
    private static final Map<CustTier, Double> DEFAULT_COMPOSITION = Map.of(
            CustTier.STANDARD, 0.60,
            CustTier.SILVER, 0.20,
            CustTier.GOLD, 0.15,
            CustTier.DIAMOND, 0.05);

    private final OrderTransactionRepository transactionRepo;
    private final SimulationExecutor simulationExecutor;

    public SimulatorService(OrderTransactionRepository transactionRepo) {
        this.transactionRepo = Objects.requireNonNull(transactionRepo, "transactionRepo");
        this.simulationExecutor = new SimulationExecutor();
    }

    public SimulationResult runSimulation(LockType lockType, int numThreads, int stock) {
        return runSimulation(
                lockType, numThreads, stock,
                MAX_RETRY, DEFAULT_COMPOSITION);
    }

    public SimulationResult runSimulation(
            LockType lockType,
            int numThreads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        validateConfiguration(lockType, numThreads, stock, tierComposition);
        int retryLimit = Math.max(0, Math.min(MAX_RETRY, maxRetries));
        Path runDirectory = createRunDirectory();

        try {
            FlashItemRepository itemRepository = new FlashItemRepository(runDirectory.toString());
            itemRepository.save(createInitialItem(stock));
            SimulationExecutor.Outcome outcome = simulationExecutor.execute(
                    itemRepository, lockType, numThreads, retryLimit, tierComposition);
            FlashItem persistedItem = itemRepository.findById(ITEM_ID);
            if (persistedItem == null) {
                throw new IllegalStateException("Simulation item disappeared from CSV storage");
            }
            transactionRepo.saveAll(outcome.transactions());
            return buildResult(lockType, numThreads, stock, persistedItem, outcome);
        } finally {
            cleanupRunDirectory(runDirectory);
        }
    }

    /** Runs all four mechanisms with a caller-selected quick load of 100-500 threads. */
    public List<SimulationResult> runQuickBenchmark(int threads, int stock) {
        if (threads < QUICK_MIN_THREADS || threads > QUICK_MAX_THREADS) {
            throw new IllegalArgumentException("Quick benchmark threads must be between 100 and 500");
        }
        return runMechanismSet(threads, stock, 1);
    }

    /** Runs the PDF-required matrix: 1000 threads x 4 mechanisms x 3 repetitions. */
    public List<SimulationResult> runFullBenchmark(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        List<SimulationResult> results = new ArrayList<>(LockType.values().length * FULL_REPEATS);
        for (int runNumber = 1; runNumber <= FULL_REPEATS; runNumber++) {
            results.addAll(runMechanismSet(FULL_THREADS, stock, runNumber));
        }
        return results;
    }

    private List<SimulationResult> runMechanismSet(int threads, int stock, int runNumber) {
        List<SimulationResult> results = new ArrayList<>(LockType.values().length);
        for (LockType lockType : LockType.values()) {
            SimulationResult result = runSimulation(lockType, threads, stock);
            result.setRunNumber(runNumber);
            results.add(result);
        }
        return results;
    }

    private SimulationResult buildResult(
            LockType lockType,
            int numThreads,
            int stock,
            FlashItem persistedItem,
            SimulationExecutor.Outcome outcome) {
        int expectedFinalStock = stock - outcome.successfulQuantity();
        boolean consistent = expectedFinalStock >= 0
                && persistedItem.getSoldQty() == outcome.successfulQuantity()
                && persistedItem.getRemainingStock() == expectedFinalStock;
        long durationMs = Math.max(
                1L, TimeUnit.NANOSECONDS.toMillis(outcome.durationNanos()));
        double elapsedSeconds = durationMs / 1000.0;

        SimulationResult result = new SimulationResult();
        result.setLockType(lockType.name());
        result.setRunNumber(1);
        result.setTotalThreads(numThreads);
        result.setInitialStock(stock);
        result.setSuccessCount(outcome.successCount());
        result.setSuccessfulQuantity(outcome.successfulQuantity());
        result.setFailedCount(outcome.failedCount());
        result.setFinalStock(persistedItem.getRemainingStock());
        result.setNegativeStock(Math.max(0, -expectedFinalStock));
        result.setDurationMs(durationMs);
        result.setTps(outcome.successCount() / elapsedSeconds);
        result.setRetryCount(outcome.retryCount());
        result.setDataConsistent(consistent);
        return result;
    }

    private FlashItem createInitialItem(int stock) {
        return new FlashItem(
                ITEM_ID, "P-SIM-001", "EV-SIM-001",
                "Flash Sale Simulation Product", 500_000, 199_000, stock);
    }

    private void validateConfiguration(
            LockType lockType,
            int numThreads,
            int stock,
            Map<CustTier, Double> tierComposition) {
        Objects.requireNonNull(lockType, "lockType");
        if (numThreads <= 0 || numThreads > MAX_THREADS) {
            throw new IllegalArgumentException("numThreads must be between 1 and 1000");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        Objects.requireNonNull(tierComposition, "tierComposition");
        double totalWeight = 0.0;
        for (Map.Entry<CustTier, Double> entry : tierComposition.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || !Double.isFinite(entry.getValue()) || entry.getValue() < 0) {
                throw new IllegalArgumentException("Tier composition contains an invalid weight");
            }
            totalWeight += entry.getValue();
        }
        if (totalWeight <= 0.0) {
            throw new IllegalArgumentException("Tier composition must have a positive weight");
        }
    }

    private Path createRunDirectory() {
        try {
            return Files.createTempDirectory("flash-sale-simulation-");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create simulation workspace", exception);
        }
    }

    private void cleanupRunDirectory(Path runDirectory) {
        try {
            Files.deleteIfExists(runDirectory.resolve(ITEM_FILE_NAME + ".lck"));
            Files.deleteIfExists(runDirectory.resolve(ITEM_FILE_NAME));
            Files.deleteIfExists(runDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot clean simulation workspace: " + runDirectory, exception);
        }
    }
}
