package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import controller.FlashSaleController;
import model.FlashItem;
import repository.FlashItemRepository;

/**
 * Full JUnit Test - FlashSaleController (Tuan 5)
 * Kiem tra: getFlashSaleItems voi cac truong hop bien
 */
public class FlashSaleControllerTest {

    private static final String TEST_DIR = "test_flash_ctrl";
    private FlashItemRepository itemRepo;
    private FlashSaleController flashSaleCtrl;

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
        itemRepo = new FlashItemRepository(TEST_DIR);
        flashSaleCtrl = new FlashSaleController(itemRepo);
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    // =====================================================
    // getFlashSaleItems() - Empty repository
    // =====================================================

    @Test
    public void getItems_EmptyRepo_ReturnsNotNull() {
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertNotNull(result);
    }

    @Test
    public void getItems_EmptyRepo_ReturnsEmptyList() {
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getItems_EmptyRepo_LimitZero_ReturnsEmpty() {
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(0);
        assertTrue(result.isEmpty());
    }

    // =====================================================
    // getFlashSaleItems() - With items in repo
    // =====================================================

    @Test
    public void getItems_ThreeItems_LimitFive_ReturnsThree() {
        saveItems(3);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(5);
        assertEquals(3, result.size());
    }

    @Test
    public void getItems_FiveItems_LimitThree_ReturnsThree() {
        saveItems(5);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(3);
        assertEquals(3, result.size());
    }

    @Test
    public void getItems_FiveItems_LimitFive_ReturnsFive() {
        saveItems(5);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(5);
        assertEquals(5, result.size());
    }

    @Test
    public void getItems_TenItems_LimitOne_ReturnsOne() {
        saveItems(10);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(1);
        assertEquals(1, result.size());
    }

    @Test
    public void getItems_FiveItems_LimitZero_ReturnsEmpty() {
        saveItems(5);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getItems_NegativeLimit_ReturnsEmpty() {
        saveItems(5);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(-1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getItems_OneItem_ReturnsCorrectId() {
        FlashItem item = makeItem("FSI-00001", "P-00001");
        itemRepo.save(item);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertEquals(1, result.size());
        assertEquals("FSI-00001", result.get(0).getItemId());
    }

    @Test
    public void getItems_OneItem_CorrectProductName() {
        FlashItem item = new FlashItem("FSI-00001","P-00001","EVT-001","iPhone 15",25000000,19000000,100,0,0);
        itemRepo.save(item);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertEquals("iPhone 15", result.get(0).getProductName());
    }

    @Test
    public void getItems_OneItem_SalePriceCorrect() {
        FlashItem item = new FlashItem("FSI-00001","P-00001","EVT-001","Phone",1000000,700000,10,0,0);
        itemRepo.save(item);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertEquals(700000, result.get(0).getSalePrice());
    }

    @Test
    public void getItems_OneItem_RemainingStockCorrect() {
        FlashItem item = new FlashItem("FSI-00001","P-00001","EVT-001","Phone",1000000,700000,50,10,0);
        itemRepo.save(item);
        List<FlashItem> result = flashSaleCtrl.getFlashSaleItems(10);
        assertEquals(40, result.get(0).getRemainingStock());
    }

    @Test
    public void getItems_AfterSave_CountIncreases() {
        saveItems(2);
        int before = flashSaleCtrl.getFlashSaleItems(100).size();
        itemRepo.save(makeItem("FSI-NEW", "P-NEW"));
        int after = flashSaleCtrl.getFlashSaleItems(100).size();
        assertEquals(before + 1, after);
    }

    // =====================================================
    // Helper methods
    // =====================================================

    private void saveItems(int count) {
        for (int i = 1; i <= count; i++) {
            itemRepo.save(makeItem(String.format("FSI-%05d", i), String.format("P-%05d", i)));
        }
    }

    private FlashItem makeItem(String itemId, String productId) {
        return new FlashItem(itemId, productId, "EVT-00001", "Product " + itemId,
                1000000, 700000, 100);
    }
}

// Member 3
