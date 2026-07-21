package view;

import controller.SimulationReportController;
import controller.SimulatorController;
import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import service.SimulationReportService;
import service.SimulatorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Researcher console for the PDF-required concurrency experiments. */
public class ResearcherView {
    private final SimulatorController simulatorController;
    private final SimulationReportController reportController;
    private final SimulatorView simulatorView;
    private final ConsoleInput input;
    private final Map<CustTier, Double> tierComposition = new EnumMap<>(CustTier.class);

    private int threads = 500;
    private int stock = 100;
    private List<SimulationResult> lastResults = new ArrayList<>();

    public ResearcherView(SimulatorController simulatorController,
                          SimulationReportController reportController,
                          SimulatorView simulatorView,
                          ConsoleInput input) {
        this.simulatorController = simulatorController;
        this.reportController = reportController;
        this.simulatorView = simulatorView;
        this.input = input;
        tierComposition.put(CustTier.STANDARD, 0.60);
        tierComposition.put(CustTier.SILVER, 0.20);
        tierComposition.put(CustTier.GOLD, 0.15);
        tierComposition.put(CustTier.DIAMOND, 0.05);
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> configureQuickSimulation();
                    case 2 -> runSingle();
                    case 3 -> runQuickBenchmark();
                    case 4 -> runRequiredBenchmark();
                    case 5 -> exportReport();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác mô phỏng.");
            } catch (IllegalArgumentException exception) {
                System.out.println("Cấu hình không hợp lệ: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== MENU NGHIÊN CỨU VIÊN =====");
        System.out.printf("Quick: %d luồng, kho %d, optimistic retry tối đa %d%n",
                threads, stock, SimulatorService.MAX_RETRY);
        System.out.println("1. Cấu hình quick test (100–500 luồng)");
        System.out.println("2. Chạy một cơ chế");
        System.out.println("3. Quick benchmark 4 cơ chế");
        System.out.println("4. Benchmark bắt buộc 1000 × 4 × 3 và lấy trung bình");
        System.out.println("5. Xuất kết quả CSV + Markdown");
        System.out.println("0. Quay lại");
    }

    private void configureQuickSimulation() {
        threads = input.readIntMinMax("Số luồng", SimulatorService.QUICK_MIN_THREADS,
                SimulatorService.QUICK_MAX_THREADS, "Số luồng phải từ 100 đến 500");
        stock = input.readIntMin("Tồn kho ban đầu", 0, "Tồn kho không được âm");
        int standard = input.readIntMin("Tỷ lệ STANDARD (%)", 0, "Tỷ lệ không được âm");
        int silver = input.readIntMin("Tỷ lệ SILVER (%)", 0, "Tỷ lệ không được âm");
        int gold = input.readIntMin("Tỷ lệ GOLD (%)", 0, "Tỷ lệ không được âm");
        int diamond = input.readIntMin("Tỷ lệ DIAMOND (%)", 0, "Tỷ lệ không được âm");
        int total = standard + silver + gold + diamond;
        if (total <= 0) {
            throw new IllegalArgumentException("Tổng tỷ lệ hạng phải lớn hơn 0");
        }
        tierComposition.put(CustTier.STANDARD, standard / (double) total);
        tierComposition.put(CustTier.SILVER, silver / (double) total);
        tierComposition.put(CustTier.GOLD, gold / (double) total);
        tierComposition.put(CustTier.DIAMOND, diamond / (double) total);
    }

    private void runSingle() {
        LockType lockType = input.readLockType("Cơ chế khóa", LockType.OPTIMISTIC_LOCK);
        lastResults = List.of(simulatorController.runSingle(lockType, threads, stock,
                SimulatorService.MAX_RETRY, tierComposition));
        simulatorView.displayResults(lastResults);
    }

    private void runQuickBenchmark() {
        lastResults = simulatorController.runQuickBenchmark(threads, stock);
        simulatorView.displayResults(lastResults);
    }

    private void runRequiredBenchmark() {
        System.out.println("Đang chạy 1000 luồng × 4 cơ chế × 3 lần. Vui lòng chờ...");
        lastResults = simulatorController.runBenchmark(
                SimulatorService.FULL_THREADS, stock, SimulatorService.FULL_REPEATS,
                SimulatorService.MAX_RETRY, tierComposition);
        simulatorView.displayResults(lastResults);
    }

    private void exportReport() {
        if (lastResults.isEmpty()) {
            System.out.println("Chưa có kết quả để xuất.");
            return;
        }
        try {
            SimulationReportService.ExportedReports reports = reportController.export(lastResults);
            System.out.println("Đã xuất Markdown: " + reports.markdown());
            System.out.println("Đã xuất CSV: " + reports.csv());
        } catch (IOException exception) {
            System.out.println("Không thể xuất báo cáo: " + exception.getMessage());
        }
    }
}
