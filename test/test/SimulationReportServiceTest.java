package test;

import model.SimulationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.SimulationReportService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimulationReportServiceTest {
    private static final Path TEST_DIR = Path.of("test_data", "simulation_report");

    @Before
    public void setUp() throws Exception {
        deleteDirectory();
        Files.createDirectories(TEST_DIR);
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory();
    }

    @Test
    public void exportsCsvAndMarkdownWithRequiredMetrics() throws Exception {
        SimulationResult result = new SimulationResult();
        result.setRunNumber(1);
        result.setLockType("OPTIMISTIC_LOCK");
        result.setTotalThreads(500);
        result.setInitialStock(100);
        result.setSuccessCount(100);
        result.setSuccessfulQuantity(100);
        result.setFailedCount(400);
        result.setFinalStock(0);
        result.setNegativeStock(0);
        result.setDurationMs(1_000);
        result.setRetryCount(25);
        result.setTps(100.0);
        result.setDataConsistent(true);
        result.setVsBaselinePercent(-20.0);

        SimulationReportService.ExportedReports reports =
                new SimulationReportService(TEST_DIR).export(List.of(result));

        String csv = Files.readString(reports.csv(), StandardCharsets.UTF_8);
        String markdown = Files.readString(reports.markdown(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("retryRatePercent"));
        assertTrue(csv.contains("OPTIMISTIC_LOCK"));
        assertTrue(csv.contains("5.00"));
        assertTrue(csv.contains("DAT"));
        assertTrue(markdown.contains("Báo cáo thực nghiệm"));
        assertTrue(markdown.contains("vs Baseline"));
    }

    @Test
    public void exportsStockSpecificRawCsvAndMarkdownSummary() throws Exception {
        SimulationResult first = benchmarkResult(1, 100.0);
        SimulationResult second = benchmarkResult(2, 120.0);

        SimulationReportService.ExportedReports reports =
                new SimulationReportService(TEST_DIR)
                        .exportScenario(100, List.of(first, second));

        assertEquals("simulation_stock_100.csv", reports.csv().getFileName().toString());
        assertEquals("simulation_stock_100.md", reports.markdown().getFileName().toString());

        List<String> csvLines = Files.readAllLines(reports.csv(), StandardCharsets.UTF_8);
        String markdown = Files.readString(reports.markdown(), StandardCharsets.UTF_8);
        assertEquals(3, csvLines.size());
        assertTrue(csvLines.get(0).contains("initialStock"));
        assertTrue(csvLines.get(0).contains("retryRatePercent"));
        assertTrue(markdown.contains("Initial stock = 100"));
        assertTrue(markdown.contains("Tóm tắt theo cơ chế"));
        assertTrue(markdown.contains("110.00"));
        assertTrue(markdown.contains("Dữ liệu thô"));
    }

    private static SimulationResult benchmarkResult(int runNumber, double tps) {
        SimulationResult result = new SimulationResult();
        result.setRunNumber(runNumber);
        result.setLockType("NO_LOCK");
        result.setTotalThreads(1_000);
        result.setInitialStock(100);
        result.setSuccessCount(75);
        result.setSuccessfulQuantity(100);
        result.setFailedCount(925);
        result.setFinalStock(0);
        result.setNegativeStock(0);
        result.setDurationMs(750);
        result.setRetryCount(0);
        result.setTps(tps);
        result.setDataConsistent(true);
        result.setVsBaselinePercent(0.0);
        return result;
    }

    private static void deleteDirectory() throws Exception {
        if (!Files.exists(TEST_DIR)) {
            return;
        }
        try (var paths = Files.walk(TEST_DIR)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
