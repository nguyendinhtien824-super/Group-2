package view;

import model.SimulationResult;

import java.util.List;

public class SimulatorView {
    public void displayResults(List<SimulationResult> results) {
        if (results.isEmpty()) {
            System.out.println("Khong co ket qua simulator.");
            return;
        }

        System.out.printf("%-16s %8s %8s %8s %8s %10s %10s %12s %10s%n",
                "Co che", "Thread", "Stock", "OK", "Fail", "Con lai", "TPS", "vs Base %", "Muc tieu");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (SimulationResult result : results) {
            System.out.printf("%-16s %8d %8d %8d %8d %10d %10.0f %12.1f %10s%n",
                    result.getLockType(),
                    result.getTotalThreads(),
                    result.getInitialStock(),
                    result.getSuccessCount(),
                    result.getFailedCount(),
                    result.getFinalStock(),
                    result.getTps(),
                    result.getVsBaselinePercent(),
                    getTargetStatus(result));
        }
    }

    private String getTargetStatus(SimulationResult result) {
        if (result.getLockType().startsWith("NO_LOCK")) {
            return "BASELINE";
        }

        boolean noNegativeStock = result.getNegativeStock() == 0 && result.isDataConsistent();
        boolean throughputOk = result.getVsBaselinePercent() >= -30.0;
        return noNegativeStock && throughputOk ? "DAT" : "CHUA DAT";
    }
}
