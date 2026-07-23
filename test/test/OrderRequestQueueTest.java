package test;

import controller.OrderController;
import exception.InsufficientStockException;
import model.Customer;
import model.FlashItem;
import model.FlashSaleEvent;
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
import service.OrderRequestQueue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OrderRequestQueueTest {
    private static final DateTimeFormatter EVENT_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Path testDirectory;
    private ExecutorService executor;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("order-request-queue-test-");
        executor = Executors.newFixedThreadPool(4);
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        try (var paths = Files.walk(testDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot clean test path", exception);
                }
            });
        }
    }

    @Test
    public void vipPremiumRunsBeforeWaitingStandardRequest() throws Exception {
        OrderRequestQueue queue = new OrderRequestQueue();
        List<String> processingOrder = new CopyOnWriteArrayList<>();
        try (OrderRequestQueue.Permit blocker = queue.acquire(CustTier.STANDARD)) {
            Future<?> standard = submitQueued(queue, CustTier.STANDARD,
                    "standard", processingOrder);
            awaitPending(queue, 1);
            Future<?> vip = submitQueued(queue, CustTier.DIAMOND,
                    "vip", processingOrder);
            awaitPending(queue, 2);
            blocker.close();
            vip.get(5, TimeUnit.SECONDS);
            standard.get(5, TimeUnit.SECONDS);
        }

        assertEquals(List.of("vip", "standard"), processingOrder);
    }

    @Test
    public void samePriorityPreservesFifoSequence() throws Exception {
        OrderRequestQueue queue = new OrderRequestQueue();
        List<String> processingOrder = new CopyOnWriteArrayList<>();
        try (OrderRequestQueue.Permit blocker = queue.acquire(CustTier.STANDARD)) {
            Future<?> first = submitQueued(queue, CustTier.GOLD,
                    "first", processingOrder);
            awaitPending(queue, 1);
            Future<?> second = submitQueued(queue, CustTier.DIAMOND,
                    "second", processingOrder);
            awaitPending(queue, 2);
            blocker.close();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }

        assertEquals(List.of("first", "second"), processingOrder);
    }

    @Test
    public void vipStillFailsWhenRequestedQuantityExceedsStock() throws Exception {
        OrderRequestQueue queue = new OrderRequestQueue();
        Fixture fixture = createFixture(new FlashItemRepository(testDirectory.toString()), queue);
        fixture.customerRepository().save(customer("C-VIP", CustTier.DIAMOND));
        fixture.itemRepository().save(item(1));

        try {
            fixture.controller().bookItem("FI-QUEUE", 2, "C-VIP");
            fail("VIP/PREMIUM must not bypass the stock invariant");
        } catch (InsufficientStockException expected) {
            assertEquals(0, fixture.itemRepository().findById("FI-QUEUE").getSoldQty());
            assertTrue(fixture.orderRepository().findAll().isEmpty());
        }
    }

    @Test
    public void integratedPriorityUsesLastStockWithoutOverselling() throws Exception {
        OrderRequestQueue queue = new OrderRequestQueue();
        BlockingFlashItemRepository itemRepository =
                new BlockingFlashItemRepository(testDirectory.toString());
        Fixture fixture = createFixture(itemRepository, queue);
        fixture.customerRepository().save(customer("C-ACTIVE", CustTier.STANDARD));
        fixture.customerRepository().save(customer("C-WAITING", CustTier.STANDARD));
        fixture.customerRepository().save(customer("C-VIP", CustTier.DIAMOND));
        itemRepository.save(item(2));

        Future<Boolean> active = executor.submit(() ->
                fixture.controller().bookItem("FI-QUEUE", 1, "C-ACTIVE"));
        assertTrue(itemRepository.awaitFirstSale());

        Future<Boolean> waitingStandard = executor.submit(() ->
                fixture.controller().bookItem("FI-QUEUE", 1, "C-WAITING"));
        awaitPending(queue, 1);
        Future<Boolean> vip = executor.submit(() ->
                fixture.controller().bookItem("FI-QUEUE", 1, "C-VIP"));
        awaitPending(queue, 2);
        itemRepository.releaseFirstSale();

        assertTrue(active.get(5, TimeUnit.SECONDS));
        assertTrue(vip.get(5, TimeUnit.SECONDS));
        assertInsufficientStock(waitingStandard);
        FlashItem persisted = itemRepository.findById("FI-QUEUE");
        assertEquals(2, persisted.getSoldQty());
        assertEquals(0, persisted.getRemainingStock());
        assertEquals(List.of("C-ACTIVE", "C-VIP"), fixture.orderRepository().findAll()
                .stream().map(order -> order.getCustomerId()).toList());
    }

    private Future<?> submitQueued(OrderRequestQueue queue, CustTier tier,
                                   String label, List<String> processingOrder) {
        return executor.submit(() -> {
            try (OrderRequestQueue.Permit ignored = queue.acquire(tier)) {
                processingOrder.add(label);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Queue wait interrupted", exception);
            }
        });
    }

    private void awaitPending(OrderRequestQueue queue, int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (queue.pendingCount() != expected) {
            if (System.nanoTime() >= deadline) {
                fail("Expected " + expected + " pending requests, got " + queue.pendingCount());
            }
            Thread.sleep(5);
        }
    }

    private void assertInsufficientStock(Future<Boolean> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("The lower-priority request must fail after stock is exhausted");
        } catch (ExecutionException exception) {
            assertTrue(exception.getCause() instanceof InsufficientStockException);
        }
    }

    private Fixture createFixture(FlashItemRepository itemRepository,
                                  OrderRequestQueue queue) {
        CustomerRepository customerRepository =
                new CustomerRepository(testDirectory.toString());
        OrderRepository orderRepository = new OrderRepository(testDirectory.toString());
        OrderDetailRepository detailRepository =
                new OrderDetailRepository(testDirectory.toString());
        FlashSaleEventRepository eventRepository =
                new FlashSaleEventRepository(testDirectory.toString());
        VoucherRepository voucherRepository = new VoucherRepository(testDirectory.toString());
        OrderTransactionRepository transactionRepository =
                new OrderTransactionRepository(testDirectory.toString());
        eventRepository.save(activeEvent());
        OrderController controller = new OrderController(new FlashSaleServiceImpl(
                itemRepository, orderRepository, detailRepository, customerRepository,
                voucherRepository, transactionRepository, eventRepository, queue));
        return new Fixture(controller, itemRepository, customerRepository, orderRepository);
    }

    private static Customer customer(String id, CustTier tier) {
        return new Customer(id, "Customer " + id, id + "@test.local",
                "0900000000", "Ha Noi", "", tier);
    }

    private static FlashItem item(int stock) {
        return new FlashItem("FI-QUEUE", "P-QUEUE", "EV-QUEUE", "Queue Product",
                1_000_000, 700_000, stock);
    }

    private static FlashSaleEvent activeEvent() {
        LocalDateTime now = LocalDateTime.now();
        return new FlashSaleEvent("EV-QUEUE", "Queue Event",
                now.minusMinutes(30).format(EVENT_TIME),
                now.plusMinutes(90).format(EVENT_TIME), "ACTIVE");
    }

    private record Fixture(OrderController controller,
                           FlashItemRepository itemRepository,
                           CustomerRepository customerRepository,
                           OrderRepository orderRepository) {
    }

    private static final class BlockingFlashItemRepository extends FlashItemRepository {
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final java.util.concurrent.CountDownLatch entered =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);

        private BlockingFlashItemRepository(String dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public boolean sellWithOptimisticLock(String itemId, int quantity) {
            if (first.compareAndSet(true, false)) {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Blocked sale interrupted", exception);
                }
            }
            return super.sellWithOptimisticLock(itemId, quantity);
        }

        private boolean awaitFirstSale() throws InterruptedException {
            return entered.await(5, TimeUnit.SECONDS);
        }

        private void releaseFirstSale() {
            release.countDown();
        }
    }
}
