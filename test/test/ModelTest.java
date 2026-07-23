package test;

import org.junit.Test;
import static org.junit.Assert.*;

import model.*;
import model.enums.*;

public class ModelTest {

    @Test
    public void testCustomerModel() {
        System.out.println("--> Running: testCustomerModel");
        Customer customer = new Customer("C-00001", "Nguyen Van A", "a@gmail.com", "0912345678", "Ha Noi", "avatar1.png", CustTier.GOLD, "ACTIVE");
        assertEquals("C-00001", customer.getId());
        assertEquals("C-00001", customer.getCustomerId());
        assertEquals("Nguyen Van A", customer.getName());
        assertEquals("a@gmail.com", customer.getEmail());
        assertEquals("0912345678", customer.getPhone());
        assertEquals("Ha Noi", customer.getAddress());
        assertEquals("avatar1.png", customer.getAvatarUrl());
        assertEquals(CustTier.GOLD, customer.getTier());
        assertEquals("ACTIVE", customer.getStatus());

        customer.setCustomerId("C-99999");
        customer.setName("Nguyen Van B");
        customer.setEmail("b@gmail.com");
        customer.setPhone("0987654321");
        customer.setAddress("Ho Chi Minh");
        customer.setAvatarUrl("avatar2.png");
        customer.setTier(CustTier.DIAMOND);
        customer.setStatus("INACTIVE");
        assertEquals("C-99999", customer.getId());
        assertEquals("Nguyen Van B", customer.getName());
        assertEquals(CustTier.DIAMOND, customer.getTier());
        assertEquals("INACTIVE", customer.getStatus());

        String custCsv = customer.toCsvLine();
        Customer parsedCust = Customer.fromCsvLine(custCsv);
        assertNotNull(parsedCust);
        assertEquals("C-99999", parsedCust.getCustomerId());
        assertEquals("Nguyen Van B", parsedCust.getName());
        assertEquals("b@gmail.com", parsedCust.getEmail());
        assertEquals(CustTier.DIAMOND, parsedCust.getTier());
        assertEquals("INACTIVE", parsedCust.getStatus());
    }

    @Test
    public void testFlashItemModel() {
        System.out.println("--> Running: testFlashItemModel");
        FlashItem flashItem = new FlashItem("FI-00001", "P-00001", "EV-001", "Xiaomi Redmi Note", 5000000, 3500000, 100);
        assertEquals("FI-00001", flashItem.getId());
        assertEquals("P-00001", flashItem.getProductId());
        assertEquals("EV-001", flashItem.getEventId());
        assertEquals("Xiaomi Redmi Note", flashItem.getProductName());
        assertEquals(5000000, flashItem.getOriginalPrice());
        assertEquals(3500000, flashItem.getSalePrice());
        assertEquals(100, flashItem.getInitialStock());
        assertEquals(0, flashItem.getSoldQty());
        assertEquals(0, flashItem.getVersion());
        assertEquals(100, flashItem.getRemainingStock());

        flashItem.setSoldQty(10);
        assertEquals(90, flashItem.getRemainingStock());
        flashItem.setVersion(1);
        assertEquals(1, flashItem.getVersion());

        String itemCsv = flashItem.toCsvLine();
        FlashItem parsedItem = FlashItem.fromCsvLine(itemCsv);
        assertNotNull(parsedItem);
        assertEquals("FI-00001", parsedItem.getItemId());
        assertEquals("P-00001", parsedItem.getProductId());
        assertEquals(10, parsedItem.getSoldQty());
        assertEquals(1, parsedItem.getVersion());
    }

    @Test
    public void testFlashSaleEventModel() {
        System.out.println("--> Running: testFlashSaleEventModel");
        FlashSaleEvent event = new FlashSaleEvent("EV-001", "Flash Sale 12:00", "2025-06-15 12:00:00", "2025-06-15 14:00:00", "ACTIVE");
        assertEquals("EV-001", event.getId());
        assertEquals("Flash Sale 12:00", event.getName());
        assertEquals("2025-06-15 12:00:00", event.getStartTime());
        assertEquals("2025-06-15 14:00:00", event.getEndTime());
        assertEquals("ACTIVE", event.getStatus());

        event.setStatus("INACTIVE");
        assertEquals("INACTIVE", event.getStatus());
        String eventCsv = event.toCsvLine();
        FlashSaleEvent parsedEvent = FlashSaleEvent.fromCsvLine(eventCsv);
        assertNotNull(parsedEvent);
        assertEquals("EV-001", parsedEvent.getEventId());
        assertEquals("INACTIVE", parsedEvent.getStatus());
    }

    @Test
    public void testProductModel() {
        System.out.println("--> Running: testProductModel");
        Product product = new Product("P-00001", "Gaming Mouse", "Logitech", "Gear", 1200000, 50, "Description");
        assertEquals("P-00001", product.getId());
        assertEquals("Gaming Mouse", product.getName());
        assertEquals("Logitech", product.getBrand());
        assertEquals("Gear", product.getCategory());
        assertEquals(1200000, product.getPrice());
        assertEquals(50, product.getStock());
        assertEquals("Description", product.getDescription());

        product.setStock(45);
        String prodCsv = product.toCsvLine();
        Product parsedProd = Product.fromCsvLine(prodCsv);
        assertNotNull(parsedProd);
        assertEquals("P-00001", parsedProd.getProductId());
        assertEquals(45, parsedProd.getStock());
    }

    @Test
    public void testVoucherModel() {
        System.out.println("--> Running: testVoucherModel");
        Voucher voucher = new Voucher("V-00001", "SHOPEE10", "PERCENTAGE", 10, 50000, 100000, 100);
        assertEquals("V-00001", voucher.getId());
        assertEquals("SHOPEE10", voucher.getCode());
        assertEquals("PERCENTAGE", voucher.getType());
        assertEquals(10, voucher.getValue());
        assertEquals(50000, voucher.getMaxDiscount());
        assertEquals(100000, voucher.getMinOrderAmount());
        assertEquals(100, voucher.getRemainingUses());

        voucher.setRemainingUses(99);
        String vouchCsv = voucher.toCsvLine();
        Voucher parsedVouch = Voucher.fromCsvLine(vouchCsv);
        assertNotNull(parsedVouch);
        assertEquals("V-00001", parsedVouch.getVoucherId());
        assertEquals(99, parsedVouch.getRemainingUses());
    }

    @Test
    public void testOrderModel() {
        System.out.println("--> Running: testOrderModel");
        Order order = new Order("O-00001", "C-00001", "Test Customer", "2025-06-15", 3500000, "SUCCESS");
        assertEquals("O-00001", order.getId());
        assertEquals("C-00001", order.getCustomerId());
        assertEquals("Test Customer", order.getCustomerName());
        assertEquals("2025-06-15", order.getOrderDate());
        assertEquals(3500000, order.getTotalAmount());
        assertEquals("SUCCESS", order.getStatus());

        order.setStatus("FAILED");
        String orderCsv = order.toCsvLine();
        Order parsedOrder = Order.fromCsvLine(orderCsv);
        assertNotNull(parsedOrder);
        assertEquals("O-00001", parsedOrder.getOrderId());
        assertEquals("FAILED", parsedOrder.getStatus());
    }

    @Test
    public void testOrderDetailModel() {
        System.out.println("--> Running: testOrderDetailModel");
        OrderDetail detail = new OrderDetail("D-00001", "O-00001", "P-00001", 2, 1200000, 2400000);
        assertEquals("D-00001", detail.getId());
        assertEquals("O-00001", detail.getOrderId());
        assertEquals("P-00001", detail.getProductId());
        assertEquals(2, detail.getQuantity());
        assertEquals(1200000, detail.getUnitPrice());
        assertEquals(2400000, detail.getSubtotal());

        detail.setQuantity(3);
        detail.setSubtotal(3600000);
        String detailCsv = detail.toCsvLine();
        OrderDetail parsedDetail = OrderDetail.fromCsvLine(detailCsv);
        assertNotNull(parsedDetail);
        assertEquals("D-00001", parsedDetail.getDetailId());
        assertEquals(3, parsedDetail.getQuantity());
        assertEquals(3600000, parsedDetail.getSubtotal());
    }

    @Test
    public void testOrderTransactionModel() {
        System.out.println("--> Running: testOrderTransactionModel");
        OrderTransaction trans = new OrderTransaction("TX-00001", "O-00001", "C-00001", "FI-00001", 1, "SUCCESS", "Buy ok", 1718438400000L);
        assertEquals("TX-00001", trans.getId());
        assertEquals("O-00001", trans.getOrderId());
        assertEquals("C-00001", trans.getCustomerId());
        assertEquals("FI-00001", trans.getItemId());
        assertEquals(1, trans.getQuantity());
        assertEquals("SUCCESS", trans.getStatus());
        assertEquals("Buy ok", trans.getMessage());
        assertEquals(1718438400000L, trans.getTimestamp());

        trans.setStatus("FAILED");
        trans.setMessage("Failed");
        String transCsv = trans.toCsvLine();
        OrderTransaction parsedTrans = OrderTransaction.fromCsvLine(transCsv);
        assertNotNull(parsedTrans);
        assertEquals("TX-00001", parsedTrans.getTransactionId());
        assertEquals("FAILED", parsedTrans.getStatus());
        assertEquals("Failed", parsedTrans.getMessage());
    }

    @Test
    public void testSimulationResultModel() {
        System.out.println("--> Running: testSimulationResultModel");
        SimulationResult sim = new SimulationResult();
        sim.setLockType("OPTIMISTIC");
        sim.setTotalThreads(50);
        sim.setInitialStock(10);
        sim.setSuccessCount(10);
        sim.setFailedCount(40);
        sim.setFinalStock(0);
        sim.setNegativeStock(0);
        sim.setDurationMs(150);
        sim.setTps(333.33);
        sim.setDataConsistent(true);
        sim.setRetryCount(120);

        assertEquals("OPTIMISTIC", sim.getLockType());
        assertEquals(50, sim.getTotalThreads());
        assertEquals(10, sim.getInitialStock());
        assertEquals(10, sim.getSuccessCount());
        assertEquals(40, sim.getFailedCount());
        assertEquals(0, sim.getFinalStock());
        assertEquals(0, sim.getNegativeStock());
        assertEquals(150, sim.getDurationMs());
        assertEquals(333.33, sim.getTps(), 0.01);
        assertTrue(sim.isDataConsistent());
        assertEquals(120, sim.getRetryCount());

        String simCsv = sim.toCsvLine();
        assertTrue(simCsv.contains("OPTIMISTIC"));
        assertTrue(simCsv.contains("50"));
        assertTrue(simCsv.contains("120"));
    }
}
