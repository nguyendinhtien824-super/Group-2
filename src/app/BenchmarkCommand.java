package app;

import controller.SimulatorController;
import model.SimulationResult;
import repository.OrderTransactionRepository;
import service.SimulationReportService;
import service.SimulatorService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Runs and exports the two reproducible benchmark scenarios required by the lab. */
final class BenchmarkCommand {
    static final int REQUESTS = SimulatorService.FULL_THREADS;
    static final int REPEATS = SimulatorService.FULL_REPEATS;
    static final int[] STOCK_SCENARIOS = {100, 2_000};

    private BenchmarkCommand() {
    }

    static int run(Path outputDirectory, PrintStream output, PrintStream error) {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(error, "error");
        Path transactionWorkspace = null;
        try {
            transactionWorkspace = Files.createTempDirectory("flash-sale-benchmark-");
            SimulatorController simulator = new SimulatorController(new SimulatorService(
                    new OrderTransactionRepository(transactionWorkspace.toString())));
            SimulationReportService reportService = new SimulationReportService(outputDirectory);

            output.printf("Benchmark: %d requests x 4 mechanisms x %d runs.%n",
                    REQUESTS, REPEATS);
            for (int stock : STOCK_SCENARIOS) {
                output.printf("Running scenario with initial stock %d...%n", stock);
                List<SimulationResult> results = simulator.runFullBenchmark(stock);
                SimulationReportService.ExportedReports reports =
                        reportService.exportScenario(stock, results);
                output.println("Raw CSV: " + reports.csv().toAbsolutePath().normalize());
                output.println("Markdown summary: "
                        + reports.markdown().toAbsolutePath().normalize());
            }
            return 0;
        } catch (IOException | RuntimeException exception) {
            error.println("Benchmark failed: " + exception.getMessage());
            return 1;
        } finally {
            cleanupWorkspace(transactionWorkspace, error);
        }
    }

    private static void cleanupWorkspace(Path workspace, PrintStream error) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        try (var paths = Files.walk(workspace)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            error.println("Cannot remove temporary benchmark workspace: " + workspace);
        }
    }
}
