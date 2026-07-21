package test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import controller.CustomerController;
import model.Customer;
import repository.CustomerRepository;
import security.AdminCredentials;
import security.PasswordSecurity;
import security.SecurityEnvironment;

public class AuthenticationSecurityTest {
    private static final String CSV_HEADER =
            "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance";
    private static final String VALID_PASSWORD = "Secure123!";

    private Path testDirectory;
    private CustomerRepository customerRepository;
    private CustomerController customerController;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("shopee-auth-test-");
        customerRepository = new CustomerRepository(testDirectory.toString());
        customerController = new CustomerController(customerRepository);
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
    public void hash_UsesSaltedArgon2id() {
        String first = PasswordSecurity.hash(VALID_PASSWORD);
        String second = PasswordSecurity.hash(VALID_PASSWORD);

        assertTrue(first.startsWith("$argon2id$"));
        assertNotEquals(first, second);
        assertTrue(PasswordSecurity.matches(VALID_PASSWORD, first));
        assertFalse(PasswordSecurity.matches("WrongPassword", first));
    }

    @Test
    public void repositorySave_HashesPlaintextBeforeCsvWrite() {
        Customer customer = customer("C-00001", "save@x.com", VALID_PASSWORD);

        customerRepository.save(customer);

        String stored = customerRepository.findById("C-00001").getPassword();
        assertNotEquals(VALID_PASSWORD, stored);
        assertTrue(PasswordSecurity.isEncodedHash(stored));
    }

    @Test
    public void login_LegacyPlaintextPassword_MigratesAfterSuccessfulAuthentication() throws IOException {
        seedLegacyCustomer("legacy@x.com", "legacy123", "ACTIVE");

        Optional<Customer> authenticated = customerController.login(
                Map.of("email", "legacy@x.com", "password", "legacy123"));

        assertTrue(authenticated.isPresent());
        String migrated = customerRepository.findById("C-00001").getPassword();
        assertTrue(PasswordSecurity.isEncodedHash(migrated));
        assertTrue(PasswordSecurity.matches("legacy123", migrated));
    }

    @Test
    public void login_WrongLegacyPassword_DoesNotMigrateCsv() throws IOException {
        seedLegacyCustomer("legacy@x.com", "legacy123", "ACTIVE");

        Optional<Customer> authenticated = customerController.login(
                Map.of("email", "legacy@x.com", "password", "wrong123"));

        assertFalse(authenticated.isPresent());
        assertTrue("legacy123".equals(customerRepository.findById("C-00001").getPassword()));
    }

    @Test
    public void login_BannedCustomer_IsRejected() throws IOException {
        seedLegacyCustomer("banned@x.com", "legacy123", "BANNED");

        Optional<Customer> authenticated = customerController.login(
                Map.of("email", "banned@x.com", "password", "legacy123"));

        assertFalse(authenticated.isPresent());
    }

    @Test
    public void changePassword_VerifiesOldHashAndStoresNewHash() {
        Customer customer = customer("C-00001", "change@x.com", VALID_PASSWORD);
        customerRepository.save(customer);

        Map<String, Object> result = customerController.changePassword(
                "C-00001", VALID_PASSWORD, "NewSecure456!");

        assertTrue((Boolean) result.get("success"));
        String stored = customerRepository.findById("C-00001").getPassword();
        assertTrue(PasswordSecurity.matches("NewSecure456!", stored));
        assertFalse(PasswordSecurity.matches(VALID_PASSWORD, stored));
    }

    @Test
    public void adminCredentials_AuthenticateOnlyConfiguredHash() {
        String hash = PasswordSecurity.hash("AdminSecure123!");
        AdminCredentials credentials = AdminCredentials.from(Map.of(
                SecurityEnvironment.ADMIN_USERNAME, "operator",
                SecurityEnvironment.ADMIN_PASSWORD_HASH, hash)).orElseThrow();

        assertTrue(credentials.authenticate("operator", "AdminSecure123!"));
        assertFalse(credentials.authenticate("operator", "wrong"));
        assertFalse(credentials.authenticate("someone-else", "AdminSecure123!"));
    }

    @Test
    public void adminCredentials_PartialConfigurationFailsFast() {
        assertThrows(IllegalStateException.class, () -> AdminCredentials.from(Map.of(
                SecurityEnvironment.ADMIN_USERNAME, "operator")));
        assertTrue(AdminCredentials.from(Map.of()).isEmpty());
    }

    @Test
    public void topUpWallet_RejectsNonFiniteValues() {
        Customer customer = customer("C-00001", "wallet@x.com", VALID_PASSWORD);
        customerRepository.save(customer);

        assertFalse((Boolean) customerController.topUpWallet("C-00001", Double.NaN).get("success"));
        assertFalse((Boolean) customerController.topUpWallet("C-00001", Double.POSITIVE_INFINITY)
                .get("success"));
    }

    private void seedLegacyCustomer(String email, String password, String status) throws IOException {
        String row = "C-00001,Legacy," + email + ",0901234567,HN,,STANDARD," + status + ","
                + password + ",1000000";
        Files.writeString(
                testDirectory.resolve("customers.csv"),
                CSV_HEADER + System.lineSeparator() + row + System.lineSeparator(),
                StandardCharsets.UTF_8);
        customerRepository = new CustomerRepository(testDirectory.toString());
        customerController = new CustomerController(customerRepository);
    }

    private Customer customer(String id, String email, String password) {
        Customer customer = new Customer(id, "Test", email, "0901234567", "HN", "");
        customer.setPassword(password);
        return customer;
    }
}
