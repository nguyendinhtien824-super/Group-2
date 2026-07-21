package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import controller.CustomerController;
import model.Customer;
import model.enums.CustTier;
import repository.CustomerRepository;

/**
 * Full JUnit Test - CustomerController (Tuan 5)
 * Kiem tra: register, login, validation, ID generation
 */
public class CustomerControllerTest {

    private static final String TEST_DIR = "test_cust_ctrl";
    private static final String VALID_PASSWORD = "Secure123!";
    private CustomerRepository customerRepo;
    private CustomerController customerCtrl;

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDirectory(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        new File(TEST_DIR).mkdirs();
        customerRepo = new CustomerRepository(TEST_DIR);
        customerCtrl = new CustomerController(customerRepo);
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    // =====================================================
    // register() - Happy path
    // =====================================================

    @Test
    public void register_WithAllFields_ReturnsSuccess() {
        Map<String, String> p = map("name","An","email","an@x.com","phone","0901234567","address","HN");
        Map<String, Object> result = customerCtrl.register(p);
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    public void register_WithAllFields_MessageCorrect() {
        Map<String, String> p = map("name","An","email","an@x.com","phone","0901234567","address","HN");
        Map<String, Object> result = customerCtrl.register(p);
        assertEquals("Đăng ký thành công", result.get("message"));
    }

    @Test
    public void register_WithAllFields_CustomerSavedToCsv() {
        Map<String, String> p = map("name","An","email","an@x.com","phone","0901234567","address","HN");
        customerCtrl.register(p);
        Customer saved = customerRepo.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase("an@x.com"))
                .findFirst().orElse(null);
        assertNotNull(saved);
    }

    @Test
    public void register_WithAllFields_NameCorrect() {
        Map<String, String> p = map("name","Nguyen Van A","email","a@x.com","phone","0901234567","address","HN");
        customerCtrl.register(p);
        Customer saved = customerRepo.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase("a@x.com"))
                .findFirst().orElse(null);
        assertNotNull(saved);
        assertEquals("Nguyen Van A", saved.getName());
    }

    @Test
    public void register_WithAllFields_IdIsGenerated() {
        Map<String, String> p = map("name","An","email","an@x.com","phone","0901234567","address","HN");
        customerCtrl.register(p);
        Customer saved = customerRepo.findAll().get(0);
        assertNotNull(saved.getCustomerId());
        assertFalse(saved.getCustomerId().trim().isEmpty());
    }

    @Test
    public void register_WithAllFields_DefaultTierIsStandard() {
        Map<String, String> p = map("name","An","email","an@x.com","phone","0901234567","address","HN");
        customerCtrl.register(p);
        Customer saved = customerRepo.findAll().get(0);
        assertEquals(CustTier.STANDARD, saved.getTier());
    }

    @Test
    public void register_WithoutAddress_ReturnsSuccess() {
        Map<String, String> p = map("name","Bao","email","bao@x.com","phone","0901234568");
        Map<String, Object> result = customerCtrl.register(p);
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    public void register_SecondCustomer_IdIncremented() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901","address","HN"));
        customerCtrl.register(map("name","Bao","email","bao@x.com","phone","0902","address","HN"));
        assertEquals(2, customerRepo.findAll().size());
    }

    // =====================================================
    // register() - Duplicate email
    // =====================================================

    @Test
    public void register_DuplicateEmail_ReturnsFalse() {
        Map<String, String> p = map("name","An","email","dup@x.com","phone","0901","address","HN");
        customerCtrl.register(p);
        Map<String, Object> second = customerCtrl.register(p);
        assertFalse((Boolean) second.get("success"));
    }

    @Test
    public void register_DuplicateEmail_MessageCorrect() {
        Map<String, String> p = map("name","An","email","dup@x.com","phone","0901","address","HN");
        customerCtrl.register(p);
        Map<String, Object> second = customerCtrl.register(p);
        assertEquals("Email đã tồn tại", second.get("message"));
    }

    @Test
    public void register_DuplicateEmailCaseInsensitive_ReturnsFalse() {
        customerCtrl.register(map("name","An","email","DUP@X.COM","phone","0901","address","HN"));
        Map<String, Object> second = customerCtrl.register(
                map("name","Bao","email","dup@x.com","phone","0902","address","HN"));
        assertFalse((Boolean) second.get("success"));
    }

    @Test
    public void register_DuplicateEmail_OnlyOneCustomerSaved() {
        Map<String, String> p = map("name","An","email","dup@x.com","phone","0901","address","HN");
        customerCtrl.register(p);
        customerCtrl.register(p);
        assertEquals(1, customerRepo.findAll().size());
    }

    // =====================================================
    // register() - Missing required fields
    // =====================================================

    @Test
    public void register_MissingName_ReturnsFalse() {
        Map<String, String> p = map("email","noname@x.com","phone","0901","address","HN");
        Map<String, Object> result = customerCtrl.register(p);
        assertFalse((Boolean) result.get("success"));
    }

    @Test
    public void register_MissingEmail_ReturnsFalse() {
        Map<String, String> p = map("name","An","phone","0901","address","HN");
        Map<String, Object> result = customerCtrl.register(p);
        assertFalse((Boolean) result.get("success"));
    }

    @Test
    public void register_MissingPhone_ReturnsFalse() {
        Map<String, String> p = map("name","An","email","an@x.com","address","HN");
        Map<String, Object> result = customerCtrl.register(p);
        assertFalse((Boolean) result.get("success"));
    }

    @Test
    public void register_EmptyPayload_ReturnsFalse() {
        Map<String, Object> result = customerCtrl.register(new HashMap<>());
        assertFalse((Boolean) result.get("success"));
    }

    @Test
    public void register_MissingFields_NothingSavedToCsv() {
        Map<String, String> p = map("email","noname@x.com","phone","0901");
        customerCtrl.register(p);
        assertTrue(customerRepo.findAll().isEmpty());
    }

    @Test
    public void register_MissingPassword_ReturnsFalse() {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "An");
        payload.put("email", "an@x.com");
        payload.put("phone", "0901");

        Map<String, Object> result = customerCtrl.register(payload);

        assertFalse((Boolean) result.get("success"));
        assertTrue(customerRepo.findAll().isEmpty());
    }

    @Test
    public void register_ValidPassword_StoresOnlyHash() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901"));

        String storedPassword = customerRepo.findAll().get(0).getPassword();

        assertNotEquals(VALID_PASSWORD, storedPassword);
        assertTrue(storedPassword.startsWith("$argon2id$"));
    }

    // =====================================================
    // login() - Happy path
    // =====================================================

    @Test
    public void login_ExistingEmail_ReturnsPresent() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901","address","HN"));
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","an@x.com", "password", VALID_PASSWORD));
        assertTrue(result.isPresent());
    }

    @Test
    public void login_ExistingEmail_NameCorrect() {
        customerCtrl.register(map("name","Nguyen Van A","email","a@x.com","phone","0901","address","HN"));
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","a@x.com", "password", VALID_PASSWORD));
        assertTrue(result.isPresent());
        assertEquals("Nguyen Van A", result.get().getName());
    }

    @Test
    public void login_EmailCaseInsensitive_ReturnsPresent() {
        customerCtrl.register(map("name","An","email","An@X.COM","phone","0901","address","HN"));
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","an@x.com", "password", VALID_PASSWORD));
        assertTrue(result.isPresent());
    }

    @Test
    public void login_EmailWithTrailingSpace_ReturnsPresent() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901","address","HN"));
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","  an@x.com  ", "password", VALID_PASSWORD));
        assertTrue(result.isPresent());
    }

    // =====================================================
    // login() - Not found / invalid
    // =====================================================

    @Test
    public void login_NonExistentEmail_ReturnsEmpty() {
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","ghost@x.com", "password", VALID_PASSWORD));
        assertFalse(result.isPresent());
    }

    @Test
    public void login_EmptyEmail_ReturnsEmpty() {
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","", "password", VALID_PASSWORD));
        assertFalse(result.isPresent());
    }

    @Test
    public void login_NoEmailKey_ReturnsEmpty() {
        Optional<Customer> result = customerCtrl.login(new HashMap<>());
        assertFalse(result.isPresent());
    }

    @Test
    public void login_AfterRegisterTwoUsers_ReturnsCorrectUser() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901","address","HN"));
        customerCtrl.register(map("name","Bao","email","bao@x.com","phone","0902","address","HN"));
        Optional<Customer> result = customerCtrl.login(
                Map.of("email","bao@x.com", "password", VALID_PASSWORD));
        assertTrue(result.isPresent());
        assertEquals("Bao", result.get().getName());
    }

    @Test
    public void login_WrongPassword_ReturnsEmpty() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901"));

        Optional<Customer> result = customerCtrl.login(
                Map.of("email", "an@x.com", "password", "WrongPassword"));

        assertFalse(result.isPresent());
    }

    @Test
    public void login_MissingPassword_ReturnsEmpty() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901"));

        Optional<Customer> result = customerCtrl.login(Map.of("email", "an@x.com"));

        assertFalse(result.isPresent());
    }

    // =====================================================
    // CustomerRepository - generateNewCustomerId
    // =====================================================

    @Test
    public void generateNewCustomerId_EmptyRepo_ReturnsC00001() {
        String id = customerRepo.generateNewCustomerId();
        assertEquals("C-00001", id);
    }

    @Test
    public void generateNewCustomerId_AfterOneCustomer_ReturnsC00002() {
        customerCtrl.register(map("name","An","email","an@x.com","phone","0901","address","HN"));
        String id = customerRepo.generateNewCustomerId();
        assertEquals("C-00002", id);
    }

    // =====================================================
    // Helper
    // =====================================================

    private Map<String, String> map(String... keyValues) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            m.put(keyValues[i], keyValues[i + 1]);
        }
        m.putIfAbsent("password", VALID_PASSWORD);
        return m;
    }
}
