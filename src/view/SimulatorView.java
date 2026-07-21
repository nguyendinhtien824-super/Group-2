package view;

import model.SimulationResult;
import service.SimulatorPerformanceTarget;

import java.util.List;

public class SimulatorView {
    public void displayResults(List<SimulationResult> results) {
        if (results.isEmpty()) {
            System.out.println("Không có kết quả giả lập.");
            return;
        }

        System.out.printf("%-16s %4s %7s %7s %9s %8s %8s %7s %8s %10s %9s %11s %10s%n",
                "Cơ chế", "Lần", "Luồng", "Kho", "Thành công", "Thất bại", "Còn lại",
                "Âm kho", "Retry", "TPS", "vs Base", "Nhất quán", "Mục tiêu");
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        for (SimulationResult result : results) {
            System.out.printf("%-16s %4d %7d %7d %9d %8d %8d %7d %8d %10.0f %8.1f%% %11s %10s%n",
                    result.getLockType(),
                    result.getRunNumber(),
                    result.getTotalThreads(),
                    result.getInitialStock(),
                    result.getSuccessCount(),
                    result.getFailedCount(),
                    result.getFinalStock(),
                    result.getNegativeStock(),
                    result.getRetryCount(),
                    result.getTps(),
                    result.getVsBaselinePercent(),
                    result.isDataConsistent() ? "CÓ" : "KHÔNG",
                    getTargetStatus(result));
        }
    }

    private String getTargetStatus(SimulationResult result) {
        return switch (SimulatorPerformanceTarget.evaluate(result)) {
            case BASELINE -> "BASELINE";
            case DAT -> "ĐẠT";
            case CHUA_DAT -> "CHƯA ĐẠT";
        };
    }
}

// Member 3
