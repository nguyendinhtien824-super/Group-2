package service;

import model.SimulationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Exports reproducible simulator results without putting file I/O in a View. */
public class SimulationReportService {
    private final Path reportDirectory;

    public SimulationReportService() {
        this(Path.of("data"));
    }

    public SimulationReportService(Path reportDirectory) {
        this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
    }

    public ExportedReports export(List<SimulationResult> results) throws IOException {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Simulation results must not be empty");
        }
        Path markdown = reportDirectory.resolve("simulation_report.md");
        Path csv = reportDirectory.resolve("simulation_results.csv");
        writeMarkdown(markdown, "Interactive simulation",
                "Dữ liệu của phiên mô phỏng hiện tại.", results);
        writeCsv(csv, results);
        return new ExportedReports(markdown, csv);
    }

    /** Exports one reproducible full-benchmark scenario to stock-specific files. */
    public ExportedReports exportScenario(int stock, List<SimulationResult> results)
            throws IOException {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        validateScenarioResults(stock, results);
        String fileStem = "simulation_stock_" + stock;
        Path markdown = reportDirectory.resolve(fileStem + ".md");
        Path csv = reportDirectory.resolve(fileStem + ".csv");
        writeMarkdown(markdown, "Initial stock = " + stock,
                "1.000 requests x 4 cơ chế x 3 lần chạy; "
                        + "TPS = success / elapsed seconds.", results);
        writeCsv(csv, results);
        return new ExportedReports(markdown, csv);
    }

    private void writeMarkdown(
            Path destination,
            String scenario,
            String configuration,
            List<SimulationResult> results) throws IOException {
        try (AtomicCsvFile file = AtomicCsvFile.open(destination)) {
            BufferedWriter writer = file.writer();
            writer.write("# Báo cáo thực nghiệm Flash Sale Simulator");
            writer.newLine();
            writer.newLine();
            writer.write("Kịch bản: " + scenario);
            writer.newLine();
            writer.write("Cấu hình: " + configuration);
            writer.newLine();
            writer.write("Thời điểm xuất: " + LocalDateTime.now());
            writer.newLine();
            writer.newLine();
            writeSummaryTable(writer, results);
            writer.newLine();
            writer.write("## Dữ liệu thô theo từng lần chạy");
            writer.newLine();
            writer.newLine();
            writer.write("| Lần | Cơ chế | Luồng | OK | Fail | Kho cuối | Âm kho | Retry | Retry % | TPS | vs Baseline | Mục tiêu |");
            writer.newLine();
            writer.write("|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|");
            writer.newLine();
            for (SimulationResult result : results) {
                writer.write(String.format(Locale.ROOT,
                        "| %d | %s | %d | %d | %d | %d | %d | %d | %.2f | %.2f | %.1f%% | %s |%n",
                        result.getRunNumber(), result.getLockType(), result.getTotalThreads(),
                        result.getSuccessCount(), result.getFailedCount(), result.getFinalStock(),
                        result.getNegativeStock(), result.getRetryCount(), retryRate(result),
                        result.getTps(), result.getVsBaselinePercent(), targetStatus(result)));
            }
            file.commit();
        }
    }

    private void writeSummaryTable(BufferedWriter writer, List<SimulationResult> results)
            throws IOException {
        writer.write("## Tóm tắt theo cơ chế");
        writer.newLine();
        writer.newLine();
        writer.write("| Cơ chế | Số lần | TPS TB | TPS trung vị | OK TB | Âm kho TB | Retry TB | vs Baseline TB | Nhất quán | Mục tiêu |");
        writer.newLine();
        writer.write("|---|---:|---:|---:|---:|---:|---:|---:|---|---|");
        writer.newLine();
        for (Map.Entry<String, List<SimulationResult>> entry : groupByMechanism(results).entrySet()) {
            List<SimulationResult> runs = entry.getValue();
            double averageTps = runs.stream().mapToDouble(SimulationResult::getTps)
                    .average().orElse(0.0);
            double medianTps = medianTps(runs);
            double averageSuccess = runs.stream().mapToInt(SimulationResult::getSuccessCount)
                    .average().orElse(0.0);
            double averageNegative = runs.stream().mapToInt(SimulationResult::getNegativeStock)
                    .average().orElse(0.0);
            double averageRetries = runs.stream().mapToInt(SimulationResult::getRetryCount)
                    .average().orElse(0.0);
            double averageBaseline = runs.stream().mapToDouble(
                    SimulationResult::getVsBaselinePercent).average().orElse(0.0);
            boolean consistent = runs.stream().allMatch(SimulationResult::isDataConsistent);
            String target = entry.getKey().startsWith("NO_LOCK")
                    ? "BASELINE"
                    : consistent && averageNegative == 0.0 && averageBaseline >= -30.0
                            ? "DAT" : "CHUA_DAT";
            writer.write(String.format(Locale.ROOT,
                    "| %s | %d | %.2f | %.2f | %.1f | %.1f | %.1f | %.1f%% | %s | %s |%n",
                    entry.getKey(), runs.size(), averageTps, medianTps, averageSuccess,
                    averageNegative, averageRetries, averageBaseline,
                    consistent ? "CO" : "KHONG", target));
        }
    }

    private void writeCsv(Path destination, List<SimulationResult> results) throws IOException {
        try (AtomicCsvFile file = AtomicCsvFile.open(destination)) {
            BufferedWriter writer = file.writer();
            writer.write("runNumber,lockType,totalThreads,initialStock,successCount,successfulQuantity,failedCount,finalStock,negativeStock,durationMs,retryCount,retryRatePercent,tps,dataConsistent,vsBaselinePercent,target");
            writer.newLine();
            for (SimulationResult result : results) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%b,%.1f,%s%n",
                        result.getRunNumber(), result.getLockType(), result.getTotalThreads(),
                        result.getInitialStock(), result.getSuccessCount(),
                        result.getSuccessfulQuantity(), result.getFailedCount(),
                        result.getFinalStock(), result.getNegativeStock(), result.getDurationMs(),
                        result.getRetryCount(), retryRate(result), result.getTps(),
                        result.isDataConsistent(), result.getVsBaselinePercent(), targetStatus(result)));
            }
            file.commit();
        }
    }

    private static double retryRate(SimulationResult result) {
        return result.getTotalThreads() == 0
                ? 0.0 : result.getRetryCount() * 100.0 / result.getTotalThreads();
    }

    private static String targetStatus(SimulationResult result) {
        if (result.getLockType().startsWith("NO_LOCK")) {
            return "BASELINE";
        }
        boolean safe = result.getNegativeStock() == 0 && result.isDataConsistent();
        return safe && result.getVsBaselinePercent() >= -30.0 ? "DAT" : "CHUA_DAT";
    }

    private static Map<String, List<SimulationResult>> groupByMechanism(
            List<SimulationResult> results) {
        Map<String, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (SimulationResult result : results) {
            grouped.computeIfAbsent(result.getLockType(), ignored -> new ArrayList<>())
                    .add(result);
        }
        return grouped;
    }

    private static double medianTps(List<SimulationResult> results) {
        double[] values = results.stream().mapToDouble(SimulationResult::getTps).sorted().toArray();
        int middle = values.length / 2;
        return values.length % 2 == 0
                ? (values[middle - 1] + values[middle]) / 2.0
                : values[middle];
    }

    private static void validateScenarioResults(int stock, List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Simulation results must not be empty");
        }
        for (SimulationResult result : results) {
            if (result == null || result.getInitialStock() != stock) {
                throw new IllegalArgumentException(
                        "Every simulation result must match the scenario stock");
            }
        }
    }

    public record ExportedReports(Path markdown, Path csv) {
    }
}
