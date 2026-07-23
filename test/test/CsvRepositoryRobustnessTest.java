package test;

import model.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.ProductRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvRepositoryRobustnessTest {
    private static final Path TEST_DIR = Path.of("test_data", "csv_robustness");

    @Before
    public void setUp() throws IOException {
        deleteTestDirectory();
        Files.createDirectories(TEST_DIR);
    }

    @After
    public void tearDown() throws IOException {
        deleteTestDirectory();
    }

    @Test
    public void quotedCsvFieldsRoundTripWithoutDataLoss() {
        ProductRepository repository = new ProductRepository(TEST_DIR.toString());
        Product product = new Product("P-00001", "Tai nghe \"Pro\", Gen 2", "Sony",
                "Audio", 1500000, 10, "Có dấu phẩy, dấu nháy \" và tiếng Việt");

        repository.save(product);

        Product restored = repository.findById(product.getProductId());
        assertEquals(product.getName(), restored.getName());
        assertEquals(product.getDescription(), restored.getDescription());
    }

    @Test
    public void repositoryInstancesSharingAPathDoNotLoseConcurrentWrites() throws Exception {
        ProductRepository first = new ProductRepository(TEST_DIR.toString());
        ProductRepository second = new ProductRepository(TEST_DIR.toString());
        int recordCount = 40;
        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (int index = 1; index <= recordCount; index++) {
            int id = index;
            pool.submit(() -> repositoryFor(id, first, second).save(product(id)));
        }
        pool.shutdown();

        assertTrue("Concurrent repository work timed out", pool.awaitTermination(20, TimeUnit.SECONDS));
        assertEquals(recordCount, first.findAll().size());
    }

    @Test
    public void readsTenThousandRowsInUnderOneSecond() {
        ProductRepository repository = new ProductRepository(TEST_DIR.toString());
        List<Product> products = new ArrayList<>(10_000);
        for (int index = 1; index <= 10_000; index++) {
            products.add(product(index));
        }
        repository.saveAll(products);

        long startedAt = System.nanoTime();
        List<Product> restored = new ProductRepository(TEST_DIR.toString()).findAll();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertEquals(10_000, restored.size());
        assertTrue("CSV read took " + elapsedMillis + " ms", elapsedMillis < 1_000);
    }

    private static ProductRepository repositoryFor(int id, ProductRepository first,
                                                   ProductRepository second) {
        return id % 2 == 0 ? first : second;
    }

    private static Product product(int id) {
        return new Product(String.format("P-%05d", id), "Product " + id, "Brand",
                "Category", 100_000 + id, 10, "Description " + id);
    }

    private static void deleteTestDirectory() throws IOException {
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

// Member 3
