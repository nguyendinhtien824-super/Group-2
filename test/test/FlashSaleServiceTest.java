package test;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import controller.OrderController;
import controller.OrderTrackingController;
import model.Customer;
import model.FlashItem;
import model.Order;
import model.OrderDetail;
import model.Voucher;
import model.enums.CustTier;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;
import model.FlashSaleEvent;
import repository.FlashSaleEventRepository;

/**
 * Full JUnit Test - FlashSaleService + OrderController (Tuan 5-6)
 * Covers: tier limits, stock, banned customer, voucher, order persistence
 */
public class FlashSaleServiceTest {

    private static final String TEST_DIR = "test_service";

    private FlashItemRepository itemRepo;
    private OrderRepository orderRepo;
    private OrderDetailRepository detailRepo;
    private CustomerRepository customerRepo;
    private VoucherRepository voucherRepo;
    private OrderTransactionRepository txRepo;
    private FlashSaleEventRepository eventRepo;
    private FlashSaleService service;
    private OrderController orderCtrl;
    private OrderTrackingController trackingCtrl;

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
        java.util.Locale.setDefault(new java.util.Locale("vi", "VN"));
        deleteDirectory(new File(TEST_DIR));
        new File(TEST_DIR).mkdirs();
        itemRepo     = new FlashItemRepository(TEST_DIR);
        orderRepo    = new OrderRepository(TEST_DIR);
        detailRepo   = new OrderDetailRepository(TEST_DIR);
        customerRepo = new CustomerRepository(TEST_DIR);
        voucherRepo  = new VoucherRepository(TEST_DIR);
        txRepo       = new OrderTransactionRepository(TEST_DIR);
        eventRepo    = new FlashSaleEventRepository(TEST_DIR);

        // Save default event to prevent impact on existing tests
        FlashSaleEvent defaultEvent = new FlashSaleEvent("EVT-001", "Default Event", "2020-01-01 00:00:00", "2030-01-01 00:00:00", "ACTIVE");
        eventRepo.save(defaultEvent);

        service      = new FlashSaleServiceImpl(itemRepo, orderRepo, detailRepo, customerRepo, voucherRepo, txRepo, eventRepo);
        orderCtrl    = new OrderController(service);
        trackingCtrl = new OrderTrackingController(orderRepo, detailRepo, txRepo);
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    // ===================== HELPERS =====================

    private Customer makeCustomer(String id, String email, CustTier tier) {
        return new Customer(id, "Test " + id, email, "090" + id, "Ha Noi", "", tier);
    }

    private Customer makeBannedCustomer(String id) {
        return new Customer(id, "Banned", "banned@x.com", "090", "HN", "", CustTier.STANDARD, "BANNED");
    }

    private FlashItem makeItem(String itemId, int stock) {
        return new FlashItem(itemId, "P-00001", "EVT-001", "iPhone", 25000000, 19000000, stock);
    }

    private FlashItem makeItemWithSold(String itemId, int stock, int sold) {
        FlashItem f = new FlashItem(itemId, "P-00001", "EVT-001", "iPhone", 25000000, 19000000, stock);
        f.setSoldQty(sold);
        return f;
    }

    private FlashItem makeItemWithPrice(String itemId, int salePrice, int stock) {
        int originalPrice = Math.max(2_000_000, salePrice * 2);
        return new FlashItem(itemId, "P-00001", "EVT-001", "Phone", originalPrice, salePrice, stock);
    }

    private Voucher makePctVoucher(String id, String code, int pct, int maxDisc, int minAmt, int uses) {
        return new Voucher(id, code, "PERCENTAGE", pct, maxDisc, minAmt, uses);
    }

    private Voucher makeFixedVoucher(String id, String code, int amount, int minAmt, int uses) {
        return new Voucher(id, code, "FIXED", amount, 0, minAmt, uses);
    }

    // =====================================================
    // STANDARD tier (max 2 trong cùng sản phẩm/sự kiện)
    // =====================================================

    @Test
    public void bookItem_StandardQty1_ReturnsTrue() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "s@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
    }

    @Test
    public void bookItem_StandardQty2_ReturnsTrue() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "s@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 2, "C-00001"));
    }

    @Test
    public void bookItem_StandardQty0_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "s@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", 0, "C-00001");
            fail("Qty 0 phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_StandardQtyNegative_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "s@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", -1, "C-00001");
            fail("Qty am phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    // =====================================================
    // SILVER tier (max 2 per luot, tong 2)
    // =====================================================

    @Test
    public void bookItem_SilverQty1_ReturnsTrue() throws Exception {
        customerRepo.save(makeCustomer("C-00002", "sv@x.com", CustTier.SILVER));
        itemRepo.save(makeItem("FSI-002", 10));
        assertTrue(orderCtrl.bookItem("FSI-002", 1, "C-00002"));
    }

    @Test
    public void bookItem_SilverQty2_ReturnsTrue() throws Exception {
        customerRepo.save(makeCustomer("C-00002", "sv@x.com", CustTier.SILVER));
        itemRepo.save(makeItem("FSI-002", 10));
        assertTrue(orderCtrl.bookItem("FSI-002", 2, "C-00002"));
    }

    @Test
    public void bookItem_SilverQty3_ThrowsException() {
        customerRepo.save(makeCustomer("C-00002", "sv@x.com", CustTier.SILVER));
        itemRepo.save(makeItem("FSI-002", 10));
        try {
            orderCtrl.bookItem("FSI-002", 3, "C-00002");
            fail("SILVER chi duoc mua 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    // =====================================================
    // GOLD tier (max 2 per luot, tong 2)
    // =====================================================

    @Test
    public void bookItem_GoldQty2_ReturnsTrue() throws Exception {
        customerRepo.save(makeCustomer("C-00003", "g@x.com", CustTier.GOLD));
        itemRepo.save(makeItem("FSI-003", 10));
        assertTrue(orderCtrl.bookItem("FSI-003", 2, "C-00003"));
    }

    @Test
    public void bookItem_GoldQty3_ThrowsException() {
        customerRepo.save(makeCustomer("C-00003", "g@x.com", CustTier.GOLD));
        itemRepo.save(makeItem("FSI-003", 10));
        try {
            orderCtrl.bookItem("FSI-003", 3, "C-00003");
            fail("GOLD chi duoc mua 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    // =====================================================
    // DIAMOND tier vẫn chịu cùng giới hạn tối đa 2
    // =====================================================

    @Test
    public void bookItem_DiamondQty3_ThrowsException() {
        customerRepo.save(makeCustomer("C-00004", "d@x.com", CustTier.DIAMOND));
        itemRepo.save(makeItem("FSI-004", 10));
        try {
            orderCtrl.bookItem("FSI-004", 3, "C-00004");
            fail("DIAMOND cũng chỉ được mua tối đa 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_DiamondQty4_ThrowsException() {
        customerRepo.save(makeCustomer("C-00004", "d@x.com", CustTier.DIAMOND));
        itemRepo.save(makeItem("FSI-004", 10));
        try {
            orderCtrl.bookItem("FSI-004", 4, "C-00004");
            fail("DIAMOND cũng chỉ được mua tối đa 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    // =====================================================
    // Customer validation
    // =====================================================

    @Test
    public void bookItem_CustomerNotFound_ThrowsException() {
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-GHOST");
            fail("Customer khong ton tai");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_NullCustomerId_ThrowsException() {
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", 1, null);
            fail("null customerId phai fail");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void bookItem_BannedCustomer_ThrowsExceptionWithBannedMessage() {
        customerRepo.save(makeBannedCustomer("C-BAN"));
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-BAN");
            fail("BANNED customer phai bi chan");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("BANNED") || e.getMessage().contains("bi khoa"));
        }
    }

    @Test
    public void bookItem_ActiveCustomer_NotBlocked() throws Exception {
        customerRepo.save(new Customer("C-ACT","Active","a@x.com","090","HN","",CustTier.STANDARD,"ACTIVE"));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-ACT"));
    }

    // =====================================================
    // Item validation
    // =====================================================

    @Test
    public void bookItem_ItemNotFound_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        try {
            orderCtrl.bookItem("FSI-GHOST", 1, "C-00001");
            fail("Item khong ton tai phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_NullItemId_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        try {
            orderCtrl.bookItem(null, 1, "C-00001");
            fail("null itemId phai fail");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void bookItem_EmptyItemId_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        try {
            orderCtrl.bookItem("   ", 1, "C-00001");
            fail("empty itemId phai fail");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    // =====================================================
    // Stock control
    // =====================================================

    @Test
    public void bookItem_SufficientStock_SoldQtyIncreasesByOne() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals(1, itemRepo.findById("FSI-001").getSoldQty());
    }

    @Test
    public void bookItem_SufficientStock_RemainingStockDecreasesByOne() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals(9, itemRepo.findById("FSI-001").getRemainingStock());
    }

    @Test
    public void bookItem_ExactlyOneLeft_Succeeds() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithSold("FSI-001", 10, 9));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
    }

    @Test
    public void bookItem_ExactlyOneLeft_SoldQtyBecomesEqualToInitial() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithSold("FSI-001", 10, 9));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        FlashItem f = itemRepo.findById("FSI-001");
        assertEquals(f.getInitialStock(), f.getSoldQty());
    }

    @Test
    public void bookItem_OutOfStock_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithSold("FSI-001", 5, 5));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00001");
            fail("Het hang phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_AfterOutOfStock_SoldQtyNotExceedInitial() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithSold("FSI-001", 5, 5));
        try { orderCtrl.bookItem("FSI-001", 1, "C-00001"); } catch (Exception ignored) {}
        FlashItem f = itemRepo.findById("FSI-001");
        assertTrue(f.getSoldQty() <= f.getInitialStock());
    }

    @Test
    public void bookItem_AfterSuccess_SoldQtyNotNegative() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertTrue(itemRepo.findById("FSI-001").getSoldQty() >= 0);
    }

    // =====================================================
    // Order persistence
    // =====================================================

    @Test
    public void bookItem_Success_OneOrderSaved() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals(1, orderRepo.findAll().size());
    }

    @Test
    public void bookItem_Success_OrderStatusIsPENDING() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals("PENDING", orderRepo.findAll().get(0).getStatus());
    }

    @Test
    public void approveOrder_Success_StatusChangesToAPPROVED() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        String orderId = orderRepo.findAll().get(0).getOrderId();
        
        boolean res = orderCtrl.approveOrder(orderId);
        assertTrue(res);
        assertEquals("APPROVED", orderRepo.findAll().get(0).getStatus());
    }

    @Test
    public void cancelOrder_Success_RevertsBalanceStockAndVoucher() throws Exception {
        model.Voucher v = new model.Voucher("V-001", "DISCOUNT10", "FIXED", 10000, 10000, 20000, 5);
        voucherRepo.save(v);
        
        model.Customer customer = makeCustomer("C-00001", "c@x.com", CustTier.STANDARD);
        customer.setWalletBalance(20000000);
        customerRepo.save(customer);
        
        itemRepo.save(makeItem("FSI-001", 10));
        
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "DISCOUNT10");
        
        String orderId = orderRepo.findAll().get(0).getOrderId();
        boolean res = orderCtrl.cancelOrder(orderId);
        
        assertTrue(res);
        assertEquals("CANCELLED", orderRepo.findAll().get(0).getStatus());
        assertEquals(20000000.0, customerRepo.findById("C-00001").getWalletBalance(), 0.001);
        assertEquals(10, itemRepo.findById("FSI-001").getRemainingStock());
        assertEquals(0, itemRepo.findById("FSI-001").getSoldQty());
        assertEquals(5, voucherRepo.findById("V-001").getRemainingUses());
    }

    @Test
    public void bookItem_Success_OrderCustomerIdCorrect() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals("C-00001", orderRepo.findAll().get(0).getCustomerId());
    }

    @Test
    public void bookItem_Success_OrderDetailSaved() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertFalse(detailRepo.findAll().isEmpty());
    }

    @Test
    public void bookItem_GoldQty2_OrderDetailQuantityIs2() throws Exception {
        customerRepo.save(makeCustomer("C-00002", "g@x.com", CustTier.GOLD));
        itemRepo.save(makeItem("FSI-002", 10));
        orderCtrl.bookItem("FSI-002", 2, "C-00002");
        OrderDetail d = detailRepo.findAll().get(0);
        assertEquals(2, d.getQuantity());
    }

    @Test
    public void bookItem_TwoCustomers_TwoOrdersSaved() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c1@x.com", CustTier.STANDARD));
        customerRepo.save(makeCustomer("C-00002", "c2@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        orderCtrl.bookItem("FSI-001", 1, "C-00002");
        assertEquals(2, orderRepo.findAll().size());
    }

    @Test
    public void bookItem_Failed_NoOrderSaved() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithSold("FSI-001", 5, 5));
        try { orderCtrl.bookItem("FSI-001", 1, "C-00001"); } catch (Exception ignored) {}
        assertTrue(orderRepo.findAll().isEmpty());
    }

    // =====================================================
    // Cumulative purchase limit
    // =====================================================

    @Test
    public void bookItem_Standard_BuyTwice_SecondSucceeds() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
    }

    @Test
    public void bookItem_Standard_BuyThrice_ThirdFails() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00001");
            fail("STANDARD chi mua tong 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_Diamond_BuyThreeTimes_ThirdFails() throws Exception {
        customerRepo.save(makeCustomer("C-00004", "d@x.com", CustTier.DIAMOND));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00004");
        orderCtrl.bookItem("FSI-001", 1, "C-00004");
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00004");
            fail("DIAMOND không được vượt giới hạn cộng dồn 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_Diamond_TwoUnitOrder_AdditionalPurchaseFails() throws Exception {
        customerRepo.save(makeCustomer("C-00004", "d@x.com", CustTier.DIAMOND));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 2, "C-00004");
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00004");
            fail("DIAMOND không được vượt giới hạn cộng dồn 2");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_TwoCustomers_IndependentCumulativeLimits() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c1@x.com", CustTier.STANDARD));
        customerRepo.save(makeCustomer("C-00002", "c2@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        // C-00002 chua mua lan nao → phai ok
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00002"));
    }

    // =====================================================
    // Tier discount
    // =====================================================

    @Test
    public void bookItem_StandardTier_NoDiscount_TotalEqualsPrice() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "s@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals(1000000, orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_GoldTier_FivePercentDiscount() throws Exception {
        customerRepo.save(makeCustomer("C-00003", "g@x.com", CustTier.GOLD));
        itemRepo.save(makeItemWithPrice("FSI-003", 1000000, 10));
        orderCtrl.bookItem("FSI-003", 1, "C-00003");
        assertEquals((int)(1000000 * 0.95), orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_SilverTier_TwoPercentDiscount() throws Exception {
        customerRepo.save(makeCustomer("C-00002", "sv@x.com", CustTier.SILVER));
        itemRepo.save(makeItemWithPrice("FSI-002", 1000000, 10));
        orderCtrl.bookItem("FSI-002", 1, "C-00002");
        assertEquals((int)(1000000 * 0.98), orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_DiamondTier_TenPercentDiscount() throws Exception {
        customerRepo.save(makeCustomer("C-00004", "d@x.com", CustTier.DIAMOND));
        itemRepo.save(makeItemWithPrice("FSI-004", 1000000, 10));
        orderCtrl.bookItem("FSI-004", 1, "C-00004");
        assertEquals((int)(1000000 * 0.90), orderRepo.findAll().get(0).getTotalAmount());
    }

    // =====================================================
    // Voucher - PERCENTAGE
    // =====================================================

    @Test
    public void bookItem_PercentageVoucher_TotalLessThanBasePrice() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makePctVoucher("V-001", "SALE10", 10, 500000, 0, 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "SALE10");
        assertTrue(orderRepo.findAll().get(0).getTotalAmount() < 1000000);
    }

    @Test
    public void bookItem_PercentageVoucher10Pct_CorrectTotal() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makePctVoucher("V-001", "SALE10", 10, 200000, 0, 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "SALE10");
        assertEquals(900000, orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_PercentageVoucher_MaxDiscountCapped() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 5000000, 10));
        // 10% = 500000, nhung cap tai 100000
        voucherRepo.save(makePctVoucher("V-001", "CAPED", 10, 100000, 0, 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "CAPED");
        assertEquals(4900000, orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_PercentageVoucher_RemainingUsesDecremented() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makePctVoucher("V-001", "SALE10", 10, 200000, 0, 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "SALE10");
        assertEquals(4, voucherRepo.findById("V-001").getRemainingUses());
    }

    // =====================================================
    // Voucher - FIXED
    // =====================================================

    @Test
    public void bookItem_FixedVoucher50K_CorrectTotal() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makeFixedVoucher("V-002", "FIXED50K", 50000, 0, 3));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "FIXED50K");
        assertEquals(950000, orderRepo.findAll().get(0).getTotalAmount());
    }

    @Test
    public void bookItem_FixedVoucher_RemainingUsesDecremented() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makeFixedVoucher("V-002", "FIXED50K", 50000, 0, 3));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "FIXED50K");
        assertEquals(2, voucherRepo.findById("V-002").getRemainingUses());
    }

    // =====================================================
    // Voucher - Invalid cases
    // =====================================================

    @Test
    public void bookItem_InvalidVoucherCode_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00001", "WRONG_CODE");
            fail("Ma voucher sai phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_ExhaustedVoucher_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        voucherRepo.save(makePctVoucher("V-001", "DEAD10", 10, 200000, 0, 0));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00001", "DEAD10");
            fail("Voucher het luot phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_OrderBelowVoucherMinAmount_ThrowsException() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 500000, 10));
        voucherRepo.save(makePctVoucher("V-001", "BIG10", 10, 200000, 1000000, 5));
        try {
            orderCtrl.bookItem("FSI-001", 1, "C-00001", "BIG10");
            fail("Gia tri don hang duoi min phai fail");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void bookItem_NullVoucherCode_TreatedAsNoVoucher() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001", null));
    }

    @Test
    public void bookItem_EmptyVoucherCode_TreatedAsNoVoucher() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001", "  "));
    }

    @Test
    public void bookItem_VoucherUsed_StockStillDecrements() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItemWithPrice("FSI-001", 1000000, 10));
        voucherRepo.save(makePctVoucher("V-001", "SALE10", 10, 200000, 0, 5));
        orderCtrl.bookItem("FSI-001", 1, "C-00001", "SALE10");
        assertEquals(1, itemRepo.findById("FSI-001").getSoldQty());
    }

    // =====================================================
    // OrderTrackingController
    // =====================================================

    @Test
    public void getOrdersByCustomer_AfterBooking_ReturnsNonEmpty() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertFalse(trackingCtrl.getOrdersByCustomer("C-00001").isEmpty());
    }

    @Test
    public void getOrdersByCustomer_WrongId_ReturnsEmpty() {
        assertTrue(trackingCtrl.getOrdersByCustomer("C-GHOST").isEmpty());
    }

    @Test
    public void getOrdersByCustomer_ReturnsCorrectCustomerOrders() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c1@x.com", CustTier.STANDARD));
        customerRepo.save(makeCustomer("C-00002", "c2@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        orderCtrl.bookItem("FSI-001", 1, "C-00002");
        List<Order> orders = trackingCtrl.getOrdersByCustomer("C-00001");
        assertEquals(1, orders.size());
        assertEquals("C-00001", orders.get(0).getCustomerId());
    }

    @Test
    public void getOrderById_ValidId_ReturnsCorrectOrder() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        String orderId = orderRepo.findAll().get(0).getOrderId();
        Order o = trackingCtrl.getOrderById(orderId);
        assertNotNull(o);
        assertEquals("C-00001", o.getCustomerId());
    }

    @Test
    public void getOrderById_InvalidId_ReturnsNull() {
        assertNull(trackingCtrl.getOrderById("O-GHOST"));
    }

    @Test
    public void getOrderDetails_AfterBooking_ReturnsDetail() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        String orderId = orderRepo.findAll().get(0).getOrderId();
        assertFalse(trackingCtrl.getOrderDetails(orderId).isEmpty());
    }

    @Test
    public void getOrderDetails_InvalidId_ReturnsEmpty() {
        assertTrue(trackingCtrl.getOrderDetails("O-GHOST").isEmpty());
    }

    @Test
    public void getTransactionsByCustomer_EmptyRepo_ReturnsEmpty() {
        assertTrue(trackingCtrl.getTransactionsByCustomer("C-00001").isEmpty());
    }

    // =====================================================
    // OrderRepository - generateNewOrderId
    // =====================================================

    @Test
    public void generateNewOrderId_EmptyRepo_ReturnsO00001() {
        assertEquals("O-00001", orderRepo.generateNewOrderId());
    }

    @Test
    public void generateNewOrderId_AfterOneOrder_ReturnsO00002() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        orderCtrl.bookItem("FSI-001", 1, "C-00001");
        assertEquals("O-00002", orderRepo.generateNewOrderId());
    }

    // =====================================================
    // CustomerRepository - generateNewCustomerId
    // =====================================================

    @Test
    public void generateNewCustomerId_EmptyRepo_ReturnsC00001() {
        assertEquals("C-00001", customerRepo.generateNewCustomerId());
    }

    @Test
    public void generateNewCustomerId_OneCustomer_ReturnsC00002() {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        assertEquals(C_00002_placeholder(), customerRepo.generateNewCustomerId());
    }

    private String C_00002_placeholder() {
        return "C-00002";
    }

    // =====================================================
    // Flash Sale Event Validation (Active/Timing)
    // =====================================================

    @Test
    public void bookItem_EventNotFound_ThrowsException() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        // Item with eventId that does not exist
        FlashItem item = new FlashItem("FSI-GHOST", "P-00001", "EVT-GHOST", "Ghost Item", 25000000, 19000000, 10);
        itemRepo.save(item);
        try {
            orderCtrl.bookItem("FSI-GHOST", 1, "C-00001");
            fail("Event non-exist must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Sự kiện Flash Sale không tồn tại"));
        }
    }

    @Test
    public void bookItem_EventInactive_ThrowsException() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        // Create an inactive event
        eventRepo.save(new FlashSaleEvent("EVT-INACTIVE", "Inactive", "2020-01-01 00:00:00", "2030-01-01 00:00:00", "INACTIVE"));
        FlashItem item = new FlashItem("FSI-INACTIVE", "P-00001", "EVT-INACTIVE", "Inactive Item", 25000000, 19000000, 10);
        itemRepo.save(item);
        try {
            orderCtrl.bookItem("FSI-INACTIVE", 1, "C-00001");
            fail("Inactive event must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Sự kiện Flash Sale hiện không hoạt động"));
        }
    }

    @Test
    public void bookItem_EventUpcoming_ThrowsException() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        // Event starts in year 2099
        eventRepo.save(new FlashSaleEvent("EVT-UPCOMING", "Upcoming", "2099-01-01 00:00:00", "2099-01-02 00:00:00", "ACTIVE"));
        FlashItem item = new FlashItem("FSI-UPCOMING", "P-00001", "EVT-UPCOMING", "Upcoming Item", 25000000, 19000000, 10);
        itemRepo.save(item);
        try {
            orderCtrl.bookItem("FSI-UPCOMING", 1, "C-00001");
            fail("Upcoming event must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Sự kiện Flash Sale chưa bắt đầu"));
        }
    }

    @Test
    public void bookItem_EventExpired_ThrowsException() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        // Event ended in year 2020
        eventRepo.save(new FlashSaleEvent("EVT-EXPIRED", "Expired", "2020-01-01 00:00:00", "2020-01-02 00:00:00", "ACTIVE"));
        FlashItem item = new FlashItem("FSI-EXPIRED", "P-00001", "EVT-EXPIRED", "Expired Item", 25000000, 19000000, 10);
        itemRepo.save(item);
        try {
            orderCtrl.bookItem("FSI-EXPIRED", 1, "C-00001");
            fail("Expired event must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Sự kiện Flash Sale đã kết thúc"));
        }
    }

    @Test
    public void completeOrder_Success() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
        
        int maxId = orderRepo.findAll().stream()
                .map(Order::getOrderId)
                .filter(id -> id != null && id.startsWith("O-"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        String lastOrderId = String.format("O-%05d", maxId);
        
        // Chuyển PENDING -> APPROVED
        assertTrue(orderCtrl.approveOrder(lastOrderId));
        
        // Chuyển APPROVED -> SUCCESS
        assertTrue(orderCtrl.completeOrder(lastOrderId));
        
        Order completedOrder = orderRepo.findById(lastOrderId);
        assertEquals("SUCCESS", completedOrder.getStatus());
    }

    @Test
    public void completeOrder_InvalidState_ThrowsException() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
        
        int maxId = orderRepo.findAll().stream()
                .map(Order::getOrderId)
                .filter(id -> id != null && id.startsWith("O-"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        String lastOrderId = String.format("O-%05d", maxId);
        
        // Đơn hàng đang PENDING, cố chuyển sang SUCCESS sẽ lỗi
        try {
            orderCtrl.completeOrder(lastOrderId);
            fail("Should throw exception for PENDING order");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Chỉ đơn hàng ở trạng thái APPROVED mới có thể chuyển sang SUCCESS"));
        }
    }

    @Test
    public void cancelAllPendingAndApprovedOrders_Success() throws Exception {
        customerRepo.save(makeCustomer("C-00001", "c@x.com", CustTier.STANDARD));
        itemRepo.save(makeItem("FSI-001", 10));
        assertTrue(orderCtrl.bookItem("FSI-001", 1, "C-00001"));
        
        int maxId = orderRepo.findAll().stream()
                .map(Order::getOrderId)
                .filter(id -> id != null && id.startsWith("O-"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        String lastOrderId = String.format("O-%05d", maxId);
        
        // Đang ở trạng thái PENDING, gọi hủy hàng loạt cho C-00001
        service.cancelAllPendingAndApprovedOrders("C-00001");
        
        Order cancelledOrder = orderRepo.findById(lastOrderId);
        assertEquals("CANCELLED", cancelledOrder.getStatus());
        
        // Kiểm tra tồn kho của FlashItem đã được hoàn trả về 10
        FlashItem item = itemRepo.findById("FSI-001");
        assertEquals(10, item.getRemainingStock());
        assertEquals(0, item.getSoldQty());
    }

    @Test
    public void bookItem_LockedEvent_ThrowsException() throws Exception {
        Customer cust = makeCustomer("C-00001", "c@x.com", CustTier.STANDARD);
        customerRepo.save(cust);
        
        // 1. Tạo sự kiện LOCKED có unlockTime trong tương lai (10 phút sau)
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String unlockStr = java.time.LocalDateTime.now().plusMinutes(10).format(dtf);
        FlashSaleEvent lockedEvent = new FlashSaleEvent("EVT-LOCK", "Locked Event", "2020-01-01 00:00:00", "2030-01-01 00:00:00", "LOCKED", unlockStr);
        eventRepo.save(lockedEvent);
        
        // Tạo sản phẩm thuộc sự kiện này
        FlashItem item = new FlashItem("FSI-LOCK", "P-LOCK", "EVT-LOCK", "iPhone Locked", 25000000, 19000000, 10);
        itemRepo.save(item);
        
        // 2. Cố gắng đặt hàng -> sẽ bị ném lỗi
        try {
            orderCtrl.bookItem("FSI-LOCK", 1, "C-00001");
            fail("Nên ném ngoại lệ do sự kiện đang bị khóa");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Sự kiện Flash Sale đang bị tạm khóa"));
        }
        
        // 3. Mở khóa sự kiện -> đặt hàng thành công
        lockedEvent.setStatus("ACTIVE");
        lockedEvent.setUnlockTime("");
        eventRepo.save(lockedEvent);
        
        assertTrue(orderCtrl.bookItem("FSI-LOCK", 1, "C-00001"));
    }
}

// Member 3
