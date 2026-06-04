package view;

import controller.SimulatorController;
import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ResearcherView {
    private final SimulatorController simulatorController;
    private final SimulatorView simulatorView;
    private final ConsoleInput input;

    private int threads = 500;
    private int stock = 100;
    private int maxRetries = 100;
    private final Map<CustTier, Double> tierComposition = new HashMap<>();
    private List<SimulationResult> lastResults = new ArrayList<>();

    public ResearcherView(SimulatorController simulatorController, SimulatorView simulatorView) {
        this.simulatorController = simulatorController;
        this.simulatorView = simulatorView;
        this.input = new ConsoleInput(new Scanner(System.in));

        tierComposition.put(CustTier.STANDARD, 0.60);
        tierComposition.put(CustTier.SILVER, 0.20);
        tierComposition.put(CustTier.GOLD, 0.15);
        tierComposition.put(CustTier.DIAMOND, 0.05);
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("===== MENU NGHIEN CUU VIEN (RESEARCHER) =====");
            System.out.println("Cau hinh hien tai: " + threads + " threads, " + stock + " stock, maxRetries=" + maxRetries);
            System.out.printf("Ty le Tier: Standard=%.0f%%, Silver=%.0f%%, Gold=%.0f%%, Diamond=%.0f%%%n",
                    tierComposition.get(CustTier.STANDARD) * 100,
                    tierComposition.get(CustTier.SILVER) * 100,
                    tierComposition.get(CustTier.GOLD) * 100,
                    tierComposition.get(CustTier.DIAMOND) * 100);
            System.out.println("--------------------------------------------");
            System.out.println("1. Cau hinh tham so gia lap");
            System.out.println("2. Chay simulator mot co che khoa");
            System.out.println("3. Chay simulator tat ca co che");
            System.out.println("4. Chay Benchmark 3 lan lay trung binh");
            System.out.println("5. Xuat bao cao thuc nghiem ra file (CSV & Markdown)");
            System.out.println("0. Quay lai menu chinh");
            int choice = input.readInt("Nhap lua chon cua ban", 0);
            switch (choice) {
                case 1:
                    configureSimulation();
                    break;
                case 2:
                    runSingle();
                    break;
                case 3:
                    runAll();
                    break;
                case 4:
                    runBenchmark();
                    break;
                case 5:
                    exportReport();
                    break;
                case 0:
                    back = true;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void configureSimulation() {
        System.out.println("\n--- CAU HINH THAM SO GIA LAP ---");
        threads = input.readInt("Nhap so luong thread (luong dong thoi)", threads);
        stock = input.readInt("Nhap so luong ton kho ban dau", stock);
        maxRetries = input.readInt("Nhap so lan retry toi da cho Optimistic Lock", maxRetries);

        System.out.println("Cau hinh ty le phan bo hang thanh vien (tong bang 100%):");
        int std = input.readInt("Ty le STANDARD (%)", 60);
        int sil = input.readInt("Ty le SILVER (%)", 20);
        int gld = input.readInt("Ty le GOLD (%)", 15);
        int dia = input.readInt("Ty le DIAMOND (%)", 5);

        int total = std + sil + gld + dia;
        if (total != 100) {
            System.out.println("Canh bao: Tong ty le bang " + total + "% (khac 100%). Se tu dong chuan hoa.");
            tierComposition.put(CustTier.STANDARD, (double) std / total);
            tierComposition.put(CustTier.SILVER, (double) sil / total);
            tierComposition.put(CustTier.GOLD, (double) gld / total);
            tierComposition.put(CustTier.DIAMOND, (double) dia / total);
        } else {
            tierComposition.put(CustTier.STANDARD, std / 100.0);
            tierComposition.put(CustTier.SILVER, sil / 100.0);
            tierComposition.put(CustTier.GOLD, gld / 100.0);
            tierComposition.put(CustTier.DIAMOND, dia / 100.0);
            System.out.println("Cau hinh ty le thanh vien thanh cong.");
        }
    }

    private void runSingle() {
        LockType lockType = input.readLockType("Nhap co che khoa", LockType.OPTIMISTIC_LOCK);
        System.out.println("Dang chay gia lap co che: " + lockType.name() + "...");
        SimulationResult result = simulatorController.runSingle(lockType, threads, stock, maxRetries, tierComposition);
        lastResults = List.of(result);
        simulatorView.displayResults(lastResults);
    }

    private void runAll() {
        System.out.println("Dang chay gia lap tat ca co che...");
        lastResults = simulatorController.runAll(threads, stock, maxRetries, tierComposition);
        simulatorView.displayResults(lastResults);
    }

    private void runBenchmark() {
        int repeats = input.readInt("Nhap so lan lap Benchmark", 3);
        System.out.println("Dang chay Benchmark " + repeats + " lan lay trung binh...");
        lastResults = simulatorController.runBenchmark(threads, stock, repeats, maxRetries, tierComposition);
        simulatorView.displayResults(lastResults);
    }

    private void exportReport() {
        if (lastResults.isEmpty()) {
            System.out.println("Chua co ket qua gia lap nao. Vui long chay gia lap truoc.");
            return;
        }

        String reportDir = "data";
        String mdPath = reportDir + "/simulation_report.md";
        String csvPath = reportDir + "/simulation_reports.csv";

        try {
            Files.createDirectories(Paths.get(reportDir));

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mdPath), StandardCharsets.UTF_8)) {
                writer.write("# BAO CAO THUC NGHIEM GIA LAP FLASH SALE\n\n");
                writer.write("Ngay tao: " + new Date().toString() + "\n");
                writer.write(String.format("Tham so dau vao: %d threads, %d stock, maxRetries=%d%n%n", threads, stock, maxRetries));
                writer.write("## Ket Qua Chi Tiet\n\n");
                writer.write("| Lock Type | Success | Failed | Final Stock | Neg Stock | Time (ms) | Retries | TPS | Consistent? | vs Baseline |\n");
                writer.write("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
                for (SimulationResult r : lastResults) {
                    writer.write(String.format("| %s | %d | %d | %d | %d | %d | %d | %.0f | %s | %.1f%% |%n",
                            r.getLockType(), r.getSuccessCount(), r.getFailedCount(), r.getFinalStock(),
                            r.getNegativeStock(), r.getDurationMs(), r.getRetryCount(), r.getTps(),
                            r.isDataConsistent() ? "YES" : "NO", r.getVsBaselinePercent()));
                }
                writer.write("\n\n*Bao cao duoc xuat tu dong boi he thong Shopee Console Simulation.*");
            }

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvPath), StandardCharsets.UTF_8)) {
                writer.write("lockType,successCount,failedCount,finalStock,negativeStock,durationMs,retryCount,tps,dataConsistent,vsBaselinePercent");
                writer.newLine();
                for (SimulationResult r : lastResults) {
                    writer.write(String.format("%s,%d,%d,%d,%d,%d,%d,%.0f,%b,%.1f",
                            r.getLockType(), r.getSuccessCount(), r.getFailedCount(), r.getFinalStock(),
                            r.getNegativeStock(), r.getDurationMs(), r.getRetryCount(), r.getTps(),
                            r.isDataConsistent(), r.getVsBaselinePercent()));
                    writer.newLine();
                }
            }

            System.out.println("Da xuat bao cao thanh cong ra:");
            System.out.println("- Markdown: data/simulation_report.md");
            System.out.println("- CSV: data/simulation_reports.csv");

        } catch (IOException e) {
            System.out.println("Loi khi xuat bao cao: " + e.getMessage());
        }
    }
}
