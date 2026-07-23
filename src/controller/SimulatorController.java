package controller;

import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import service.SimulatorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Controller coordinating simulator and benchmark use cases. */
public class SimulatorController {
    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = Objects.requireNonNull(simulatorService, "simulatorService");
    }

    public SimulationResult runSingle(LockType lockType, int threads, int stock) {
        return simulatorService.runSimulation(lockType, threads, stock);
    }

    public SimulationResult runSingle(
            LockType lockType,
            int threads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return simulatorService.runSimulation(
                lockType, threads, stock, maxRetries, tierComposition);
    }

    public List<SimulationResult> runAll(int threads, int stock) {
        return applyBaseline(runAllInternal(threads, stock, null, null));
    }

    public List<SimulationResult> runAll(
            int threads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return applyBaseline(runAllInternal(
                threads, stock, maxRetries, tierComposition));
    }

    public List<SimulationResult> runQuickBenchmark(int threads, int stock) {
        return applyBaseline(simulatorService.runQuickBenchmark(threads, stock));
    }

    /** Returns all 12 raw runs from the required 1000 x 4 x 3 matrix. */
    public List<SimulationResult> runFullBenchmark(int stock) {
        return applyBaselinePerRun(simulatorService.runFullBenchmark(stock));
    }

    public List<SimulationResult> runBenchmark(int threads, int stock, int repeats) {
        return runBenchmarkInternal(threads, stock, repeats, null, null);
    }

    public List<SimulationResult> runBenchmark(
            int threads,
            int stock,
            int repeats,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return runBenchmarkInternal(
                threads, stock, repeats, maxRetries, tierComposition);
    }

    public List<LockType> getMechanisms() {
        return Arrays.asList(LockType.values());
    }

    private List<SimulationResult> runAllInternal(
            int threads,
            int stock,
            Integer maxRetries,
            Map<CustTier, Double> tierComposition) {
        List<SimulationResult> results = new ArrayList<>(LockType.values().length);
        for (LockType lockType : LockType.values()) {
            SimulationResult result = maxRetries == null
                    ? simulatorService.runSimulation(lockType, threads, stock)
                    : simulatorService.runSimulation(
                            lockType, threads, stock, maxRetries, tierComposition);
            results.add(result);
        }
        return results;
    }

    private List<SimulationResult> runBenchmarkInternal(
            int threads,
            int stock,
            int repeats,
            Integer maxRetries,
            Map<CustTier, Double> tierComposition) {
        if (repeats <= 0) {
            throw new IllegalArgumentException("repeats must be positive");
        }
        Map<LockType, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (LockType lockType : LockType.values()) {
            grouped.put(lockType, new ArrayList<>());
        }
        for (int runNumber = 1; runNumber <= repeats; runNumber++) {
            for (SimulationResult result : runAllInternal(
                    threads, stock, maxRetries, tierComposition)) {
                LockType lockType = LockType.valueOf(result.getLockType());
                result.setRunNumber(runNumber);
                grouped.get(lockType).add(result);
            }
        }

        List<SimulationResult> averages = new ArrayList<>(LockType.values().length);
        for (Map.Entry<LockType, List<SimulationResult>> entry : grouped.entrySet()) {
            averages.add(average(entry.getKey(), entry.getValue(), threads, stock));
        }
        return applyBaseline(averages);
    }

    private List<SimulationResult> applyBaselinePerRun(List<SimulationResult> results) {
        for (int runNumber = 1; runNumber <= SimulatorService.FULL_REPEATS; runNumber++) {
            List<SimulationResult> currentRun = new ArrayList<>();
            for (SimulationResult result : results) {
                if (result.getRunNumber() == runNumber) {
                    currentRun.add(result);
                }
            }
            applyBaseline(currentRun);
        }
        return results;
    }

    private List<SimulationResult> applyBaseline(List<SimulationResult> results) {
        double baselineTps = results.stream()
                .filter(result -> result.getLockType().startsWith(LockType.NO_LOCK.name()))
                .mapToDouble(SimulationResult::getTps)
                .findFirst()
                .orElse(0.0);
        for (SimulationResult result : results) {
            if (baselineTps > 0.0) {
                double percent = ((result.getTps() - baselineTps) / baselineTps) * 100.0;
                result.setVsBaselinePercent(Math.round(percent * 10.0) / 10.0);
            }
        }
        return results;
    }

    private SimulationResult average(
            LockType lockType,
            List<SimulationResult> results,
            int threads,
            int stock) {
        int count = results.size();
        SimulationResult average = new SimulationResult();
        average.setLockType(lockType.name() + "_AVG");
        average.setTotalThreads(threads);
        average.setInitialStock(stock);
        average.setSuccessCount(roundAverage(
                results.stream().mapToInt(SimulationResult::getSuccessCount).sum(), count));
        average.setSuccessfulQuantity(roundAverage(
                results.stream().mapToInt(SimulationResult::getSuccessfulQuantity).sum(), count));
        average.setFailedCount(roundAverage(
                results.stream().mapToInt(SimulationResult::getFailedCount).sum(), count));
        average.setFinalStock(roundAverage(
                results.stream().mapToInt(SimulationResult::getFinalStock).sum(), count));
        average.setNegativeStock(roundAverage(
                results.stream().mapToInt(SimulationResult::getNegativeStock).sum(), count));
        average.setDurationMs(Math.round(
                results.stream().mapToLong(SimulationResult::getDurationMs).average().orElse(0.0)));
        average.setRetryCount(roundAverage(
                results.stream().mapToInt(SimulationResult::getRetryCount).sum(), count));
        average.setTps(results.stream().mapToDouble(SimulationResult::getTps).average().orElse(0.0));
        average.setDataConsistent(results.stream().allMatch(SimulationResult::isDataConsistent));
        return average;
    }

    private int roundAverage(int total, int count) {
        return Math.round((float) total / count);
    }
}

// Member 3
