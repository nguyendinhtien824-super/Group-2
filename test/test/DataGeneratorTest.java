package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import model.Customer;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import service.DataGeneratorService;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;

public class DataGeneratorTest {

    private static final String TEST_DIR = "test_data";

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        deleteDirectory(new File("tuan 4/" + TEST_DIR));
        deleteDirectory(new File("tuan 3/" + TEST_DIR));
        new File(TEST_DIR).mkdirs();
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
        deleteDirectory(new File("tuan 4/" + TEST_DIR));
        deleteDirectory(new File("tuan 3/" + TEST_DIR));
    }

    @Test
    public void testDataGeneratorService() throws IOException {
        System.out.println("--> Running: testDataGeneratorService");
        String path = TEST_DIR + "/generator";

        DataGeneratorService generator = new DataGeneratorService(path);
        Map<String, Integer> counts = generator.generateAll();

        assertNotNull(counts);
        assertTrue(counts.containsKey("products.csv"));
        assertTrue(counts.containsKey("customers.csv"));
        assertTrue(counts.containsKey("vouchers.csv"));
        assertTrue(counts.containsKey("flash_events.csv"));
        assertTrue(counts.containsKey("flash_items.csv"));
        assertTrue(counts.containsKey("orders.csv"));
        assertTrue(counts.containsKey("order_details.csv"));
        assertTrue(counts.containsKey("TOTAL"));
        assertTrue(counts.get("TOTAL") >= 12_500);

        String[] files = {"products.csv", "customers.csv", "vouchers.csv", "flash_events.csv", "flash_items.csv", "orders.csv", "order_details.csv"};
        for (String file : files) {
            File f = new File(path, file);
            assertTrue("File " + file + " should exist", f.exists());
            assertTrue("File " + file + " should not be empty", f.length() > 0);
        }

        List<String> productLines = Files.readAllLines(
                Path.of(path, "products.csv"), StandardCharsets.UTF_8);
        assertEquals("productId,name,brand,category,price,stock,description,version",
                productLines.get(0));
        assertEquals(5_001, productLines.size());

        List<String> customerLines = Files.readAllLines(
                Path.of(path, "customers.csv"), StandardCharsets.UTF_8);
        assertEquals(2_001, customerLines.size());
        CustomerRepository customerRepository = new CustomerRepository(path);
        List<Customer> customers = customerRepository.findAll();
        assertEquals(2_000, customers.size());
        assertTrue(customers.get(0).getPassword().startsWith("$argon2id$"));
        assertEquals(10_000_000.0, customers.get(0).getWalletBalance(), 0.0);

        FlashSaleService service = new FlashSaleServiceImpl(
                new FlashItemRepository(path), new OrderRepository(path),
                new OrderDetailRepository(path), customerRepository,
                new VoucherRepository(path), new OrderTransactionRepository(path),
                new FlashSaleEventRepository(path));
        assertNotNull(service);

        List<String> eventLines = Files.readAllLines(
                Path.of(path, "flash_events.csv"), StandardCharsets.UTF_8);
        String[] event = eventLines.get(1).split(",", -1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        long duration = Duration.between(
                LocalDateTime.parse(event[2], formatter),
                LocalDateTime.parse(event[3], formatter)).toMinutes();
        assertTrue(duration >= 60 && duration <= 120);

        List<String> orderLines = Files.readAllLines(
                Path.of(path, "orders.csv"), StandardCharsets.UTF_8);
        assertEquals("orderId,customerId,customerName,orderDate,totalAmount,status,eventId",
                orderLines.get(0));
        assertEquals(7, orderLines.get(1).split(",", -1).length);

        Path transactions = Path.of(path, "transactions.csv");
        Files.writeString(transactions,
                "TX-KEEP,O-1,C-1,FI-1,1,SUCCESS,persist,1" + System.lineSeparator(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        Map<String, Integer> regenerated = generator.generateAll();
        assertEquals(Integer.valueOf(1), regenerated.get("transactions.csv"));
        assertTrue(Files.readString(transactions, StandardCharsets.UTF_8).contains("TX-KEEP"));
    }
}

// Member 3
