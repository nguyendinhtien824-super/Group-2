package test;

import config.FlashSaleFormats;
import controller.OrderController;
import controller.OrderTrackingController;
import model.Customer;
import model.FlashItem;
import model.FlashSaleEvent;
import model.enums.CustTier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Two independent role sessions verify the Customer -> Admin -> Customer workflow. */
public class MultiRoleWorkflowTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void customerAndAdminSessionsObserveTheSameOrderLifecycle() throws Exception {
        String dataDirectory = temporaryFolder.newFolder("multi-role").getAbsolutePath();
        FlashItemRepository itemRepository = new FlashItemRepository(dataDirectory);
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(dataDirectory);
        CustomerRepository customerRepository = new CustomerRepository(dataDirectory);
        OrderRepository orderRepository = new OrderRepository(dataDirectory);
        OrderDetailRepository detailRepository = new OrderDetailRepository(dataDirectory);
        OrderTransactionRepository transactionRepository = new OrderTransactionRepository(dataDirectory);
        VoucherRepository voucherRepository = new VoucherRepository(dataDirectory);
        ProductRepository productRepository = new ProductRepository(dataDirectory);

        LocalDateTime now = LocalDateTime.now().withNano(0);
        eventRepository.save(new FlashSaleEvent(
                "EV-MULTI", "Multi-role Sale",
                now.minusMinutes(10).format(FlashSaleFormats.EVENT_TIME),
                now.plusMinutes(80).format(FlashSaleFormats.EVENT_TIME),
                "ACTIVE"));
        itemRepository.save(new FlashItem(
                "FI-MULTI", "P-MULTI", "EV-MULTI", "Concurrent Item",
                1_000_000, 500_000, 2));
        customerRepository.save(new Customer(
                "C-MULTI", "Customer Session", "multi@example.com",
                "0901234567", "Hà Nội", "", CustTier.STANDARD));

        FlashSaleService service = new FlashSaleServiceImpl(
                itemRepository, orderRepository, detailRepository,
                customerRepository, voucherRepository, transactionRepository,
                eventRepository);
        OrderController customerOrderController = new OrderController(service);
        OrderController adminOrderController = new OrderController(service);
        OrderTrackingController customerTrackingController = new OrderTrackingController(
                orderRepository, detailRepository, transactionRepository,
                productRepository, itemRepository);

        CountDownLatch simultaneousStart = new CountDownLatch(1);
        CountDownLatch orderCreated = new CountDownLatch(1);
        CountDownLatch adminFinished = new CountDownLatch(1);
        ExecutorService sessions = Executors.newFixedThreadPool(2);
        try {
            Future<String> customerSession = sessions.submit(() -> {
                simultaneousStart.await();
                assertTrue(customerOrderController.bookItem(
                        "FI-MULTI", 1, "C-MULTI"));
                orderCreated.countDown();
                assertTrue(adminFinished.await(5, TimeUnit.SECONDS));
                return customerTrackingController.getOrdersByCustomer("C-MULTI")
                        .get(0).getStatus();
            });

            Future<String> adminSession = sessions.submit(() -> {
                simultaneousStart.await();
                assertTrue(orderCreated.await(5, TimeUnit.SECONDS));
                String orderId = orderRepository.findAll().get(0).getOrderId();
                assertTrue(adminOrderController.approveOrder(orderId));
                adminFinished.countDown();
                return orderRepository.findById(orderId).getStatus();
            });

            simultaneousStart.countDown();
            assertEquals("APPROVED", adminSession.get(10, TimeUnit.SECONDS));
            assertEquals("APPROVED", customerSession.get(10, TimeUnit.SECONDS));
        } finally {
            sessions.shutdownNow();
            assertTrue(sessions.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
