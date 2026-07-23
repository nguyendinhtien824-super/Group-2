package test;

import controller.AdminReportController;
import model.Customer;
import model.Order;
import model.OrderTransaction;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.CustomerRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import service.AdminReportService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AdminReportServiceTest {
    private Path testDirectory;
    private OrderRepository orderRepository;
    private CustomerRepository customerRepository;
    private VoucherRepository voucherRepository;
    private OrderTransactionRepository transactionRepository;
    private AdminReportService service;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("admin-report-");
        orderRepository = new OrderRepository(testDirectory.toString());
        customerRepository = new CustomerRepository(testDirectory.toString());
        voucherRepository = new VoucherRepository(testDirectory.toString());
        transactionRepository = new OrderTransactionRepository(testDirectory.toString());
        service = new AdminReportService(orderRepository, customerRepository,
                voucherRepository, transactionRepository);
    }

    @After
    public void tearDown() throws IOException {
        if (testDirectory == null || !Files.exists(testDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(testDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    public void generate_CountsRevenueOnlyForSuccessAndBuildsTierVoucherMetrics() {
        seedCustomers();
        seedOrdersAndTransactions();
        voucherRepository.save(new Voucher(
                "V-00001", "SAVE10", "PERCENTAGE", 10, 50_000, 0, 8));

        AdminReportService.Report report = service.generate();

        assertEquals(120_000L, report.totalRevenue());
        assertEquals(1L, report.successfulOrders());
        assertEquals(2L, report.otherOrders());
        assertEquals(Long.valueOf(1), report.ordersByStatus().get(OrderStatus.CANCELLED));
        assertEquals(Long.valueOf(1), report.customersByTier().get(CustTier.GOLD));
        assertEquals(Long.valueOf(1), report.customersByTier().get(CustTier.STANDARD));
        assertEquals(1L, report.vouchers().get(0).successfulUses());
    }

    @Test
    public void report_IsImmutableAndControllerReturnsReport() {
        AdminReportService.Report report = service.generate();

        assertThrows(UnsupportedOperationException.class,
                () -> report.ordersByStatus().put(OrderStatus.SUCCESS, 99L));
        AdminReportController.Result result = new AdminReportController(service).getReport();
        assertTrue(result.success());
        assertEquals(0L, result.report().totalRevenue());
    }

    private void seedCustomers() {
        Customer standard = new Customer("C-00001", "An", "an@example.com",
                "0901234567", "Hà Nội", "", CustTier.STANDARD, "ACTIVE");
        standard.setPassword("Secure123!");
        Customer gold = new Customer("C-00002", "Bình", "binh@example.com",
                "0911234567", "Đà Nẵng", "", CustTier.GOLD, "ACTIVE");
        gold.setPassword("Secure456!");
        customerRepository.saveAll(java.util.List.of(standard, gold));
    }

    private void seedOrdersAndTransactions() {
        orderRepository.save(new Order("O-00001", "C-00001", "An", "2026-07-18",
                120_000, OrderStatus.SUCCESS, "E-1"));
        orderRepository.save(new Order("O-00002", "C-00002", "Bình", "2026-07-18",
                900_000, OrderStatus.CANCELLED, "E-1"));
        orderRepository.save(new Order("O-00003", "C-00002", "Bình", "2026-07-18",
                500_000, OrderStatus.PENDING, "E-1"));
        transactionRepository.save(new OrderTransaction("TX-00001", "O-00001", "C-00001",
                "I-1", 1, "PENDING", "Đặt hàng | Voucher: SAVE10", 1L));
        transactionRepository.save(new OrderTransaction("TX-00002", "O-00002", "C-00002",
                "I-1", 1, "PENDING", "Đặt hàng | Voucher: SAVE10", 2L));
    }
}
