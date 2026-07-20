package test;

import controller.ProductController;
import exception.InvalidProductException;
import model.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.ProductRepository;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class ProductRequirementsTest {
    private static final String TEST_DIR = "test_product_requirements";

    private ProductController controller;

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        controller = new ProductController(new ProductRepository(TEST_DIR));
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    @Test
    public void crudAndVersionUseOptimisticUpdate() {
        Product product = product("P-1", "Gear", 1_000_000);
        controller.createProduct(product);

        Product loaded = controller.findProduct("P-1");
        assertEquals(0, loaded.getVersion());
        loaded.setPrice(900_000);
        assertTrue(controller.updateProduct(loaded, 0));
        assertEquals(1, controller.findProduct("P-1").getVersion());

        loaded.setPrice(800_000);
        assertFalse(controller.updateProduct(loaded, 0));
        assertEquals(900_000, controller.findProduct("P-1").getPrice());
        assertTrue(controller.deleteProduct("P-1"));
        assertNull(controller.findProduct("P-1"));
    }

    @Test
    public void searchFiltersByCategoryAndInclusivePriceRange() {
        controller.createProduct(product("P-1", "Gear", 500_000));
        controller.createProduct(product("P-2", "Gear", 1_500_000));
        controller.createProduct(product("P-3", "Phone", 1_000_000));

        List<Product> result = controller.searchProducts("gear", 500_000, 1_000_000);
        assertEquals(1, result.size());
        assertEquals("P-1", result.get(0).getProductId());
    }

    @Test(expected = InvalidProductException.class)
    public void duplicateProductIdIsRejected() {
        controller.createProduct(product("P-1", "Gear", 500_000));
        controller.createProduct(product("P-1", "Gear", 600_000));
    }

    @Test(expected = InvalidProductException.class)
    public void invalidPriceRangeIsRejected() {
        controller.searchProducts("Gear", 1_000_000, 500_000);
    }

    private static Product product(String id, String category, int price) {
        return new Product(id, "Product " + id, "Brand", category, price, 10, "Description");
    }

    private static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
