package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import model.*;
import repository.*;

public class CustomRepositoryTest {

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
    public void testGenerateCustomerId() {
        System.out.println("--> Running: testGenerateCustomerId");
        String path = TEST_DIR + "/genCustId";
        CustomerRepository customerRepo = new CustomerRepository(path);
        
        assertEquals("C-00001", customerRepo.generateNewCustomerId());

        customerRepo.save(new Customer("C-00005", "Customer 5", "c5@gmail.com", "09", "HN", ""));
        assertEquals("C-00006", customerRepo.generateNewCustomerId());

        customerRepo.save(new Customer("C-00100", "Customer 100", "c100@gmail.com", "09", "HN", ""));
        assertEquals("C-00101", customerRepo.generateNewCustomerId());
    }

    @Test
    public void testGenerateOrderId() {
        System.out.println("--> Running: testGenerateOrderId");
        String path = TEST_DIR + "/genOrderId";
        OrderRepository orderRepo = new OrderRepository(path);
        
        assertEquals("O-00001", orderRepo.generateNewOrderId());

        orderRepo.save(new Order("O-00015", "C-00001", "Test Customer", "2025-06-15", 50000, "SUCCESS"));
        assertEquals("O-00016", orderRepo.generateNewOrderId());
    }

    @Test
    public void testGenerateTransactionId() {
        System.out.println("--> Running: testGenerateTransactionId");
        String path = TEST_DIR + "/genTransId";
        OrderTransactionRepository transRepo = new OrderTransactionRepository(path);
        
        assertEquals("TX-00001", transRepo.generateNewTransactionId());

        transRepo.save(new OrderTransaction("TX-00250", "O-00001", "C-00001", "FI-00001", 1, "SUCCESS", "ok", 0));
        assertEquals("TX-00251", transRepo.generateNewTransactionId());
    }

    @Test
    public void testFindVoucherByCode() {
        System.out.println("--> Running: testFindVoucherByCode");
        String path = TEST_DIR + "/findVoucher";
        VoucherRepository voucherRepo = new VoucherRepository(path);
        
        assertNull(voucherRepo.findByCode("SHOPEE10"));

        Voucher v1 = new Voucher("V-00001", "SHOPEE10", "PERCENTAGE", 10, 50000, 0, 100);
        Voucher v2 = new Voucher("V-00002", "FREESHIP", "FIXED", 30000, 30000, 100000, 50);
        voucherRepo.saveAll(Arrays.asList(v1, v2));

        Voucher foundCode = voucherRepo.findByCode("shopee10");
        assertNotNull(foundCode);
        assertEquals("V-00001", foundCode.getVoucherId());

        Voucher foundFreeship = voucherRepo.findByCode("FREESHIP");
        assertNotNull(foundFreeship);
        assertEquals("V-00002", foundFreeship.getVoucherId());

        assertNull(voucherRepo.findByCode("NOT_EXIST"));
    }
}
