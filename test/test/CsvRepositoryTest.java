package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import model.Product;
import repository.ProductRepository;

public class CsvRepositoryTest {

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
    public void testFindAllEmptyInitially() {
        System.out.println("--> Running: testFindAllEmptyInitially");
        String path = TEST_DIR + "/findAllEmpty";
        ProductRepository productRepo = new ProductRepository(path);
        List<Product> list = productRepo.findAll();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSave() {
        System.out.println("--> Running: testSave");
        String path = TEST_DIR + "/save";
        ProductRepository productRepo = new ProductRepository(path);

        Product p1 = new Product("P-00001", "Keyboard", "Razer", "Gear", 2500000, 20, "High end mechanical keyboard");
        productRepo.save(p1);

        List<Product> list = productRepo.findAll();
        assertEquals(1, list.size());
        assertEquals("P-00001", list.get(0).getProductId());
        assertEquals("Keyboard", list.get(0).getName());
    }

    @Test
    public void testFindById() {
        System.out.println("--> Running: testFindById");
        String path = TEST_DIR + "/findById";
        ProductRepository productRepo = new ProductRepository(path);

        Product p1 = new Product("P-00001", "Keyboard", "Razer", "Gear", 2500000, 20, "High end mechanical keyboard");
        productRepo.save(p1);

        Product found = productRepo.findById("P-00001");
        assertNotNull(found);
        assertEquals("Keyboard", found.getName());

        Product notFound = productRepo.findById("P-99999");
        assertNull(notFound);
    }

    @Test
    public void testUpdate() {
        System.out.println("--> Running: testUpdate");
        String path = TEST_DIR + "/update";
        ProductRepository productRepo = new ProductRepository(path);

        Product p1 = new Product("P-00001", "Keyboard", "Razer", "Gear", 2500000, 20, "High end mechanical keyboard");
        productRepo.save(p1);

        p1.setStock(15);
        p1.setName("Keyboard Razer V2");
        boolean updateSuccess = productRepo.update(p1, 0);
        assertTrue(updateSuccess);

        Product updated = productRepo.findById("P-00001");
        assertNotNull(updated);
        assertEquals(15, updated.getStock());
        assertEquals("Keyboard Razer V2", updated.getName());
    }

    @Test
    public void testSaveAll() {
        System.out.println("--> Running: testSaveAll");
        String path = TEST_DIR + "/saveAll";
        ProductRepository productRepo = new ProductRepository(path);

        Product p1 = new Product("P-00001", "Keyboard", "Razer", "Gear", 2500000, 20, "High end mechanical keyboard");
        Product p2 = new Product("P-00002", "Mouse", "Razer", "Gear", 1500000, 30, "RGB Gaming Mouse");
        productRepo.saveAll(Arrays.asList(p1, p2));

        List<Product> list = productRepo.findAll();
        assertEquals(2, list.size());
        assertEquals("Keyboard", productRepo.findById("P-00001").getName());
        assertEquals("Mouse", productRepo.findById("P-00002").getName());
    }

    @Test
    public void testDeleteById() {
        System.out.println("--> Running: testDeleteById");
        String path = TEST_DIR + "/deleteById";
        ProductRepository productRepo = new ProductRepository(path);

        Product p1 = new Product("P-00001", "Keyboard", "Razer", "Gear", 2500000, 20, "High end mechanical keyboard");
        productRepo.save(p1);

        boolean deleteSuccess = productRepo.deleteById("P-00001");
        assertTrue(deleteSuccess);
        assertNull(productRepo.findById("P-00001"));

        boolean deleteFailed = productRepo.deleteById("P-99999");
        assertFalse(deleteFailed);
    }
}

// Member 3
