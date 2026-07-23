package test;

import controller.OrderController;
import exception.InsufficientStockException;
import exception.InvalidOrderException;
import exception.InvalidOrderStateException;
import exception.PurchaseLimitExceededException;
import model.Customer;
import model.FlashItem;
import model.FlashSaleEvent;
import model.Order;
import model.enums.CustTier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import service.FlashSaleServiceImpl;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class OrderBusinessRequirementsTest {
    private static final String TEST_DIR = "test_order_requirements";

    private FlashItemRepository itemRepository;
    private OrderRepository orderRepository;
    private OrderDetailRepository detailRepository;
    private CustomerRepository customerRepository;
    private FlashSaleEventRepository eventRepository;
    private OrderController controller;

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        itemRepository = new FlashItemRepository(TEST_DIR);
        orderRepository = new OrderRepository(TEST_DIR);
        detailRepository = new OrderDetailRepository(TEST_DIR);
        customerRepository = new CustomerRepository(TEST_DIR);
        eventRepository = new FlashSaleEventRepository(TEST_DIR);
        VoucherRepository voucherRepository = new VoucherRepository(TEST_DIR);
        OrderTransactionRepository transactionRepository =
                new OrderTransactionRepository(TEST_DIR);
        controller = new OrderController(new FlashSaleServiceImpl(
                itemRepository, orderRepository, detailRepository, customerRepository,
                voucherRepository, transactionRepository, eventRepository));
        eventRepository.save(activeEvent("EVT-A"));
        eventRepository.save(activeEvent("EVT-B"));
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    @Test
    public void everyTierHasSameMaximumTwoUnits() throws Exception {
        int index = 0;
        for (CustTier tier : CustTier.values()) {
            String customerId = "C-" + index;
            String itemId = "FI-" + index;
            customerRepository.save(customer(customerId, tier));
            itemRepository.save(item(itemId, "P-" + index, "EVT-A", 10));
            try {
                controller.bookItem(itemId, 3, customerId);
                fail("Mọi hạng thành viên đều phải bị chặn ở số lượng 3");
            } catch (InvalidOrderException expected) {
                assertTrue(expected.getMessage().contains("tối đa")
                        || expected.getMessage().contains("từ 1 đến 2"));
            }
            index++;
        }
    }

    @Test
    public void cumulativeLimitIsScopedByEvent() throws Exception {
        customerRepository.save(customer("C-1", CustTier.DIAMOND));
        itemRepository.save(item("FI-A", "P-SAME", "EVT-A", 10));
        itemRepository.save(item("FI-B", "P-SAME", "EVT-B", 10));

        assertTrue(controller.bookItem("FI-A", 2, "C-1"));
        assertTrue(controller.bookItem("FI-B", 2, "C-1"));
        assertEquals(2, orderRepository.findAll().size());
        assertTrue(orderRepository.findAll().stream()
                .anyMatch(order -> "EVT-A".equals(order.getEventId())));
        assertTrue(orderRepository.findAll().stream()
                .anyMatch(order -> "EVT-B".equals(order.getEventId())));
    }

    @Test
    public void cumulativeCheckAndPersistenceShareCriticalFlow() throws Exception {
        customerRepository.save(customer("C-1", CustTier.STANDARD));
        itemRepository.save(item("FI-A", "P-1", "EVT-A", 10));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger limitFailures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> bookConcurrently(ready, start, successes, limitFailures));
            Future<?> second = executor.submit(() -> bookConcurrently(ready, start, successes, limitFailures));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(1, limitFailures.get());
        assertEquals(1, orderRepository.findAll().size());
        assertEquals(2, detailRepository.findAll().get(0).getQuantity());
    }

    @Test
    public void vipPriorityNeverBypassesStock() throws Exception {
        customerRepository.save(customer("C-VIP", CustTier.DIAMOND));
        itemRepository.save(item("FI-A", "P-1", "EVT-A", 1));
        try {
            controller.bookItem("FI-A", 2, "C-VIP");
            fail("VIP không được phép bán vượt kho");
        } catch (InsufficientStockException expected) {
            assertTrue(orderRepository.findAll().isEmpty());
            assertEquals(0, itemRepository.findById("FI-A").getSoldQty());
        }
    }

    @Test
    public void successOrderCannotBeCancelled() throws Exception {
        customerRepository.save(customer("C-1", CustTier.STANDARD));
        itemRepository.save(item("FI-A", "P-1", "EVT-A", 10));
        controller.bookItem("FI-A", 1, "C-1");
        Order order = orderRepository.findAll().get(0);
        controller.approveOrder(order.getOrderId());
        controller.completeOrder(order.getOrderId());
        double balanceBefore = customerRepository.findById("C-1").getWalletBalance();
        int soldBefore = itemRepository.findById("FI-A").getSoldQty();

        try {
            controller.cancelOrder(order.getOrderId());
            fail("Đơn SUCCESS không được hủy");
        } catch (InvalidOrderStateException expected) {
            assertEquals(balanceBefore,
                    customerRepository.findById("C-1").getWalletBalance(), 0.0);
            assertEquals(soldBefore, itemRepository.findById("FI-A").getSoldQty());
        }
    }

    @Test
    public void cancelledOrderCannotBeCancelledTwice() throws Exception {
        customerRepository.save(customer("C-1", CustTier.STANDARD));
        itemRepository.save(item("FI-A", "P-1", "EVT-A", 10));
        controller.bookItem("FI-A", 1, "C-1");
        String orderId = orderRepository.findAll().get(0).getOrderId();
        controller.cancelOrder(orderId);
        double balanceBefore = customerRepository.findById("C-1").getWalletBalance();

        try {
            controller.cancelOrder(orderId);
            fail("Đơn CANCELLED không được hủy lần hai");
        } catch (InvalidOrderStateException expected) {
            assertEquals(balanceBefore,
                    customerRepository.findById("C-1").getWalletBalance(), 0.0);
            assertEquals(0, itemRepository.findById("FI-A").getSoldQty());
        }
    }

    private void bookConcurrently(CountDownLatch ready, CountDownLatch start,
                                  AtomicInteger successes, AtomicInteger limitFailures) {
        ready.countDown();
        try {
            start.await();
            controller.bookItem("FI-A", 2, "C-1");
            successes.incrementAndGet();
        } catch (PurchaseLimitExceededException expected) {
            limitFailures.incrementAndGet();
        } catch (Exception unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    private static Customer customer(String id, CustTier tier) {
        return new Customer(id, "Customer " + id, id + "@x.com", "090", "HN", "", tier);
    }

    private static FlashItem item(String id, String productId, String eventId, int stock) {
        return new FlashItem(id, productId, eventId, "Product", 1_000_000, 700_000, stock);
    }

    private static FlashSaleEvent activeEvent(String id) {
        return new FlashSaleEvent(id, "Event " + id,
                "2020-01-01 00:00:00", "2030-01-01 00:00:00", "ACTIVE");
    }

    private static void deleteDirectory(File directory) {
        if (!directory.exists()) return;
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                if (file.isDirectory()) deleteDirectory(file);
                else file.delete();
            });
        }
        directory.delete();
    }
}
