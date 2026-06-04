package controller;

import model.SimulationResult;
import model.enums.LockType;
import service.SimulatorService;

import java.util.*;

/**
 * Controller dieu phoi simulator cho console view.
 */
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    public SimulationResult runSingle(LockType lockType, int threads, int stock) {
        return simulatorService.runSimulation(lockType, threads, stock);
    }

    public SimulationResult runSingle(LockType lockType, int threads, int stock, int maxRetries, Map<model.enums.CustTier, Double> tierComposition) {
        return simulatorService.runSimulation(lockType, threads, stock, maxRetries, tierComposition);
    }

    public List<SimulationResult> runAll(int threads, int stock) {
        List<SimulationResult> results = new ArrayList<>();
        double baselineTps = 0;

        for (LockType lockType : LockType.values()) {
            SimulationResult result = simulatorService.runSimulation(lockType, threads, stock);

            if (lockType == LockType.NO_LOCK) {
                baselineTps = result.getTps();
            }

            results.add(result);
        }

        // Tinh vs Baseline %
        for (SimulationResult r : results) {
            if (baselineTps > 0) {
                double vsPercent = ((r.getTps() - baselineTps) / baselineTps) * 100;
                r.setVsBaselinePercent(Math.round(vsPercent * 10.0) / 10.0);
            }
        }

        return results;
    }

    public List<SimulationResult> runAll(int threads, int stock, int maxRetries, Map<model.enums.CustTier, Double> tierComposition) {
        List<SimulationResult> results = new ArrayList<>();
        double baselineTps = 0;

        for (LockType lockType : LockType.values()) {
            SimulationResult result = simulatorService.runSimulation(lockType, threads, stock, maxRetries, tierComposition);

            if (lockType == LockType.NO_LOCK) {
                baselineTps = result.getTps();
            }

            results.add(result);
        }

        // Tinh vs Baseline %
        for (SimulationResult r : results) {
            if (baselineTps > 0) {
                double vsPercent = ((r.getTps() - baselineTps) / baselineTps) * 100;
                r.setVsBaselinePercent(Math.round(vsPercent * 10.0) / 10.0);
            }
        }

        return results;
    }

    public List<SimulationResult> runBenchmark(int threads, int stock, int repeats) {
        Map<LockType, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (LockType lockType : LockType.values()) {
            grouped.put(lockType, new ArrayList<>());
        }

        for (int i = 0; i < repeats; i++) {
            for (LockType lockType : LockType.values()) {
                grouped.get(lockType).add(simulatorService.runSimulation(lockType, threads, stock));
            }
        }

        List<SimulationResult> averages = new ArrayList<>();
        double baselineTps = 0;
        for (Map.Entry<LockType, List<SimulationResult>> entry : grouped.entrySet()) {
            SimulationResult average = average(entry.getKey(), entry.getValue(), threads, stock);
            if (entry.getKey() == LockType.NO_LOCK) {
                baselineTps = average.getTps();
            }
            averages.add(average);
        }

        for (SimulationResult result : averages) {
            if (baselineTps > 0) {
                double vsPercent = ((result.getTps() - baselineTps) / baselineTps) * 100;
                result.setVsBaselinePercent(Math.round(vsPercent * 10.0) / 10.0);
            }
        }

        return averages;
    }

    public List<SimulationResult> runBenchmark(int threads, int stock, int repeats, int maxRetries, Map<model.enums.CustTier, Double> tierComposition) {
        Map<LockType, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (LockType lockType : LockType.values()) {
            grouped.put(lockType, new ArrayList<>());
        }

        for (int i = 0; i < repeats; i++) {
            for (LockType lockType : LockType.values()) {
                grouped.get(lockType).add(simulatorService.runSimulation(lockType, threads, stock, maxRetries, tierComposition));
            }
        }

        List<SimulationResult> averages = new ArrayList<>();
        double baselineTps = 0;
        for (Map.Entry<LockType, List<SimulationResult>> entry : grouped.entrySet()) {
            SimulationResult average = average(entry.getKey(), entry.getValue(), threads, stock);
            if (entry.getKey() == LockType.NO_LOCK) {
                baselineTps = average.getTps();
            }
            averages.add(average);
        }

        for (SimulationResult result : averages) {
            if (baselineTps > 0) {
                double vsPercent = ((result.getTps() - baselineTps) / baselineTps) * 100;
                result.setVsBaselinePercent(Math.round(vsPercent * 10.0) / 10.0);
            }
        }

        return averages;
    }

    public List<LockType> getMechanisms() {
        return Arrays.asList(LockType.values());
    }

    private SimulationResult average(LockType lockType, List<SimulationResult> results, int threads, int stock) {
        SimulationResult average = new SimulationResult();
        average.setLockType(lockType.name() + "_AVG");
        average.setTotalThreads(threads);
        average.setInitialStock(stock);
        average.setSuccessCount(roundAvg(results.stream().mapToInt(SimulationResult::getSuccessCount).sum(), results.size()));
        average.setFailedCount(roundAvg(results.stream().mapToInt(SimulationResult::getFailedCount).sum(), results.size()));
        average.setFinalStock(roundAvg(results.stream().mapToInt(SimulationResult::getFinalStock).sum(), results.size()));
        average.setNegativeStock(roundAvg(results.stream().mapToInt(SimulationResult::getNegativeStock).sum(), results.size()));
        average.setDurationMs(roundAvgLong(results.stream().mapToLong(SimulationResult::getDurationMs).sum(), results.size()));
        average.setRetryCount(roundAvg(results.stream().mapToInt(SimulationResult::getRetryCount).sum(), results.size()));
        average.setTps(Math.round(results.stream().mapToDouble(SimulationResult::getTps).average().orElse(0)));
        average.setDataConsistent(results.stream().allMatch(SimulationResult::isDataConsistent));
        return average;
    }

    private int roundAvg(int total, int count) {
        return Math.round((float) total / count);
    }

    private long roundAvgLong(long total, int count) {
        return Math.round((double) total / count);
    }
}

