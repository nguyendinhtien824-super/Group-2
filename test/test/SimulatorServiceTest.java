package test;

import model.OrderTransaction;
import model.SimulationResult;
import model.enums.LockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.OrderTransactionRepository;
import controller.SimulatorController;
import service.SimulatorPerformanceTarget;
import service.SimulatorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimulatorServiceTest {
    private Path testDirectory;
    private OrderTransactionRepository transactionRepository;
    private SimulatorService simulatorService;
    private SimulatorController simulatorController;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("simulator-service-test-");
        transactionRepository = new OrderTransactionRepository(testDirectory.toString());
        simulatorService = new SimulatorService(transactionRepository);
        simulatorController = new SimulatorController(simulatorService);
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
    public void quickBenchmarkRunsFourCsvMechanismsAndPersistsTransactions() {
        List<SimulationResult> results = simulatorController.runQuickBenchmark(100, 30);

        assertEquals(4, results.size());
        assertResultMatrix(results, 100, 1);
        List<OrderTransaction> transactions = transactionRepository.findAll();
        assertEquals(400, transactions.size());
        assertTrue(Files.exists(testDirectory.resolve("transactions.csv")));
        assertTrue(transactions.stream().allMatch(transaction ->
                transaction.getQuantity() >= 1 && transaction.getQuantity() <= 2));
        SimulationResult noLock = results.stream()
                .filter(result -> LockType.NO_LOCK.name().equals(result.getLockType()))
                .findFirst()
                .orElseThrow();
        assertTrue(noLock.getNegativeStock() > 0);
        assertTrue(!noLock.isDataConsistent());
    }

    @Test(timeout = 300_000)
    public void fullBenchmarkRunsRequiredOneThousandByFourByThreeMatrix() {
        List<SimulationResult> results = simulatorController.runFullBenchmark(2_000);

        assertEquals(12, results.size());
        assertResultMatrix(results, 1000, 3);
        assertEquals(12_000, transactionRepository.findAll().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void quickBenchmarkRejectsThreadCountBelowOneHundred() {
        simulatorService.runQuickBenchmark(99, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void quickBenchmarkRejectsThreadCountAboveFiveHundred() {
        simulatorService.runQuickBenchmark(501, 10);
    }

    private void assertResultMatrix(
            List<SimulationResult> results,
            int expectedThreads,
            int expectedRunsPerMechanism) {
        Map<LockType, Integer> mechanismCounts = new EnumMap<>(LockType.class);
        for (SimulationResult result : results) {
            LockType lockType = LockType.valueOf(result.getLockType());
            mechanismCounts.merge(lockType, 1, Integer::sum);
            assertEquals(expectedThreads, result.getTotalThreads());
            assertEquals(expectedThreads,
                    result.getSuccessCount() + result.getFailedCount());
            assertTrue(result.getDurationMs() >= 1);
            double expectedTps = result.getSuccessCount()
                    / (result.getDurationMs() / 1000.0);
            assertEquals(expectedTps, result.getTps(), 0.0001);
            if (lockType.protectsStock()) {
                assertEquals(0, result.getNegativeStock());
                assertTrue(result.getFinalStock() >= 0);
                assertTrue(result.isDataConsistent());
                SimulatorPerformanceTarget.Verdict expectedVerdict =
                        result.getVsBaselinePercent()
                                        >= -SimulatorPerformanceTarget.MAX_THROUGHPUT_DROP_PERCENT
                                ? SimulatorPerformanceTarget.Verdict.DAT
                                : SimulatorPerformanceTarget.Verdict.CHUA_DAT;
                assertEquals(expectedVerdict, SimulatorPerformanceTarget.evaluate(result));
            }
        }
        for (LockType lockType : LockType.values()) {
            assertEquals(Integer.valueOf(expectedRunsPerMechanism),
                    mechanismCounts.get(lockType));
        }
    }
}
