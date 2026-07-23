package test;

import controller.AdminCustomerController;
import model.Customer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.CustomerRepository;
import security.PasswordSecurity;
import service.CustomerAdminService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CustomerAdminServiceTest {
    private Path testDirectory;
    private CustomerRepository repository;
    private AtomicReference<String> cancelledCustomer;
    private CustomerAdminService service;
    private AdminCustomerController controller;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("customer-admin-");
        repository = new CustomerRepository(testDirectory.toString());
        cancelledCustomer = new AtomicReference<>();
        service = new CustomerAdminService(repository, cancelledCustomer::set);
        controller = new AdminCustomerController(service);
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
    public void create_ValidatesAndStoresArgon2Password() {
        Customer customer = service.create("Nguyễn An", "an@example.com", "0901234567",
                "Hà Nội", "", "gold", "Secure123!");

        Customer stored = repository.findById(customer.getCustomerId());
        assertEquals("GOLD", stored.getTier().name());
        assertTrue(PasswordSecurity.isEncodedHash(stored.getPassword()));
        assertTrue(PasswordSecurity.matches("Secure123!", stored.getPassword()));
        assertEquals(1, service.search("example.com").size());
    }

    @Test
    public void create_RejectsMissingPasswordAndDuplicateEmail() {
        AdminCustomerController.Result<Customer> missingPassword = controller.create(
                "Nguyễn An", "an@example.com", "0901234567", "Hà Nội", "", "", "");
        assertFalse(missingPassword.success());

        service.create("Nguyễn An", "an@example.com", "0901234567",
                "Hà Nội", "", "STANDARD", "Secure123!");
        assertThrows(IllegalArgumentException.class, () -> service.create(
                "Nguyễn Bình", "AN@example.com", "0911234567",
                "Đà Nẵng", "", "SILVER", "Secure456!"));
    }

    @Test
    public void setStatus_ValidatesStatusAndCancelsBeforeBan() {
        Customer customer = createCustomer("ban@example.com");

        Customer banned = service.setStatus(customer.getCustomerId(), "banned");

        assertEquals("BANNED", banned.getStatus());
        assertEquals(customer.getCustomerId(), cancelledCustomer.get());
        assertThrows(IllegalArgumentException.class,
                () -> service.setStatus(customer.getCustomerId(), "DELETED"));
    }

    @Test
    public void delete_CancelsOrdersBeforeRemovingCustomer() {
        Customer customer = createCustomer("delete@example.com");

        Customer deleted = service.delete(customer.getCustomerId());

        assertEquals(customer.getCustomerId(), deleted.getCustomerId());
        assertEquals(customer.getCustomerId(), cancelledCustomer.get());
        assertNull(repository.findById(customer.getCustomerId()));
    }

    @Test
    public void delete_CancellationFailurePreservesCustomerAndReturnsClearError() {
        Customer customer = createCustomer("keep@example.com");
        CustomerAdminService failingService = new CustomerAdminService(repository, id -> {
            throw new IllegalStateException("order failure");
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> failingService.delete(customer.getCustomerId()));

        assertTrue(exception.getMessage().contains("PENDING/APPROVED"));
        assertTrue(repository.findById(customer.getCustomerId()) != null);
    }

    private Customer createCustomer(String email) {
        return service.create("Khách Hàng", email, "0901234567", "Hà Nội",
                "", "STANDARD", "Secure123!");
    }
}

// Member 3
