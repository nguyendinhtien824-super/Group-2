import model.FlashItem;
import model.Product;
import model.SimulationResult;
import model.enums.LockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import controller.SimulatorController;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.OrderRepository;
import repository.OrderDetailRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;
import service.DataGeneratorService;
import service.FlashSaleServiceImpl;
import service.SimulatorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlashSaleConsoleAppTest {

    @TempDir
    Path tempDir;

    @Test
    void dataGeneratorCreatesRequiredCsvRows() throws IOException {
        DataGeneratorService generator = new DataGeneratorService(tempDir.toString());

        Map<String, Integer> counts = generator.generateAll();

        assertEquals(5000, counts.get("products.csv"));
        assertEquals(2000, counts.get("customers.csv"));
        assertEquals(10, counts.get("flash_events.csv"));
        assertEquals(500, counts.get("flash_items.csv"));
        assertEquals(2500, counts.get("orders.csv"));
        assertEquals(2500, counts.get("order_details.csv"));
        assertEquals(0, counts.get("transactions.csv"));
        assertTrue(counts.get("TOTAL") >= 10000);
        assertEquals(5001, countLines("products.csv"));
        assertEquals(501, countLines("flash_items.csv"));
        assertEquals(1, countLines("transactions.csv"));
    }

    @Test
    void flashItemRepositoryPreventsOversellWithSafeLocks() {
        FlashItemRepository repository = new FlashItemRepository(tempDir.toString());
        repository.save(new FlashItem("FI-001", "P-001", "EV-001", "Test Product", 1000, 500, 3));

        assertTrue(repository.sellWithOptimisticLock("FI-001", 2));
        assertFalse(repository.sellWithOptimisticLock("FI-001", 2));
        assertEquals(1, repository.findById("FI-001").getRemainingStock());

        assertTrue(repository.sellWithSynchronized("FI-001", 1));
        assertFalse(repository.sellWithFileLock("FI-001", 1));
        assertEquals(0, repository.findById("FI-001").getRemainingStock());
    }

    @Test
    void productRepositorySupportsCrudAndSearchByCategoryPrice() {
        ProductRepository repository = new ProductRepository(tempDir.toString());
        Product phone = new Product("P-001", "Phone A", "Brand", "Phone", 2_000_000, 10, "Good");
        Product laptop = new Product("P-002", "Laptop A", "Brand", "Laptop", 15_000_000, 5, "Fast");

        repository.save(phone);
        repository.save(laptop);
        phone.setPrice(1_800_000);

        assertTrue(repository.update(phone, 0));
        List<Product> phones = repository.searchByCategoryAndPrice("phone", 1_000_000, 3_000_000);
        assertEquals(1, phones.size());
        assertEquals("P-001", phones.get(0).getProductId());
        assertTrue(repository.deleteById("P-002"));
        assertNull(repository.findById("P-002"));
    }

    @Test
    void flashSaleServiceValidatesOrderQuantityAndStock() throws Exception {
        FlashItemRepository repository = new FlashItemRepository(tempDir.toString());
        repository.save(new FlashItem("FI-001", "P-001", "EV-001", "Test Product", 1000, 500, 2));
        OrderRepository orderRepository = new OrderRepository(tempDir.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(tempDir.toString());
        CustomerRepository customerRepository = new CustomerRepository(tempDir.toString());
        VoucherRepository voucherRepository = new VoucherRepository(tempDir.toString());

        customerRepository.save(new model.Customer("C-00001", "Test Customer", "test@gmail.com", "0123456789", "Ha Noi", "", model.enums.CustTier.GOLD));

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(repository, orderRepository, orderDetailRepository, customerRepository, voucherRepository);

        assertTrue(service.bookItem("FI-001", 2, "C-00001"));
        assertThrows(Exception.class, () -> service.bookItem("FI-001", 1, "C-00001"));
        assertThrows(Exception.class, () -> service.bookItem("FI-001", 3, "C-00001"));
    }

    @Test
    void flashSaleServiceEnforcesPurchaseLimitOfTwoPerCustomer() throws Exception {
        FlashItemRepository repository = new FlashItemRepository(tempDir.toString());
        repository.save(new FlashItem("FI-001", "P-001", "EV-001", "Test Product", 1000, 500, 10));
        OrderRepository orderRepository = new OrderRepository(tempDir.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(tempDir.toString());
        CustomerRepository customerRepository = new CustomerRepository(tempDir.toString());
        VoucherRepository voucherRepository = new VoucherRepository(tempDir.toString());

        customerRepository.save(new model.Customer("C-00001", "Test Customer 1", "test1@gmail.com", "0123456789", "Ha Noi", ""));
        customerRepository.save(new model.Customer("C-00002", "Test Customer 2", "test2@gmail.com", "0123456789", "Ha Noi", "", model.enums.CustTier.GOLD));

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(repository, orderRepository, orderDetailRepository, customerRepository, voucherRepository);

        // Lần đặt 1: đặt 1 sản phẩm -> thành công
        assertTrue(service.bookItem("FI-001", 1, "C-00001"));

        // Lần đặt 2: đặt thêm 1 sản phẩm -> thành công
        assertTrue(service.bookItem("FI-001", 1, "C-00001"));

        // Lần đặt 3: đặt thêm 1 sản phẩm -> thất bại do vượt quá giới hạn 2 sản phẩm của khách hàng C-00001
        assertThrows(Exception.class, () -> service.bookItem("FI-001", 1, "C-00001"));

        // Một khách hàng khác (C-00002) vẫn có thể đặt đặt 2 sản phẩm bình thường
        assertTrue(service.bookItem("FI-001", 2, "C-00002"));
    }

    @Test
    void simulatorShowsRaceConditionAndSafeLockConsistency() {
        SimulatorService simulator = new SimulatorService(new OrderTransactionRepository(tempDir.toString()));

        SimulationResult noLock = simulator.runSimulation(LockType.NO_LOCK, 80, 5);
        assertTrue(noLock.getFinalStock() < 0, "NO_LOCK must expose negative stock under contention");

        for (LockType lockType : List.of(LockType.FILE_LOCK, LockType.SYNCHRONIZED, LockType.OPTIMISTIC_LOCK)) {
            SimulationResult result = simulator.runSimulation(lockType, 80, 5);
            assertEquals(5, result.getInitialStock() - result.getFinalStock(), lockType + " must only sell available stock");
            assertEquals(0, result.getFinalStock(), lockType + " must not go negative");
            assertTrue(result.isDataConsistent(), lockType + " must keep data consistent");
        }
    }

    @Test
    void simulatorControllerRunsThreeRepeatBenchmarkAverages() {
        SimulatorController controller = new SimulatorController(
                new SimulatorService(new OrderTransactionRepository(tempDir.toString()))
        );

        List<SimulationResult> averages = controller.runBenchmark(30, 5, 3);

        assertEquals(4, averages.size());
        assertTrue(averages.stream().anyMatch(result -> result.getLockType().equals("NO_LOCK_AVG")));
        assertTrue(averages.stream()
                .filter(result -> !result.getLockType().startsWith("NO_LOCK"))
                .allMatch(SimulationResult::isDataConsistent));
    }

    @Test
    void flashSaleServiceBlocksBannedCustomer() throws Exception {
        FlashItemRepository repository = new FlashItemRepository(tempDir.toString());
        repository.save(new FlashItem("FI-001", "P-001", "EV-001", "Test Product", 1000, 500, 10));
        OrderRepository orderRepository = new OrderRepository(tempDir.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(tempDir.toString());
        CustomerRepository customerRepository = new CustomerRepository(tempDir.toString());
        VoucherRepository voucherRepository = new VoucherRepository(tempDir.toString());

        // Tao khach hang co status BANNED
        customerRepository.save(new model.Customer("C-00001", "Banned Customer", "banned@gmail.com", "0123456789", "Ha Noi", "", model.enums.CustTier.STANDARD, "BANNED"));
        
        // Tao khach hang ACTIVE
        customerRepository.save(new model.Customer("C-00002", "Active Customer", "active@gmail.com", "0123456789", "Ha Noi", "", model.enums.CustTier.STANDARD, "ACTIVE"));

        FlashSaleServiceImpl service = new FlashSaleServiceImpl(repository, orderRepository, orderDetailRepository, customerRepository, voucherRepository);

        // Khach hang active dat duoc hang
        assertTrue(service.bookItem("FI-001", 1, "C-00002"));

        // Khach hang banned bi chan va nem loi
        Exception exception = assertThrows(Exception.class, () -> service.bookItem("FI-001", 1, "C-00001"));
        assertTrue(exception.getMessage().contains("bi khoa") || exception.getMessage().contains("BANNED"));
    }

    private long countLines(String fileName) throws IOException {
        try (var lines = Files.lines(tempDir.resolve(fileName))) {
            return lines.count();
        }
    }
}
