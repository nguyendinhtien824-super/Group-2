package test;

import config.FlashSaleFormats;
import controller.FlashSaleController;
import exception.InvalidDiscountException;
import exception.InvalidEventException;
import model.FlashItem;
import model.FlashSaleEvent;
import model.enums.SaleStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class FlashSaleLifecycleRequirementsTest {
    private static final String TEST_DIR = "test_event_requirements";

    private FlashSaleController controller;
    private LocalDateTime start;

    @Before
    public void setUp() {
        deleteDirectory(new File(TEST_DIR));
        FlashItemRepository itemRepository = new FlashItemRepository(TEST_DIR);
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(TEST_DIR);
        controller = new FlashSaleController(itemRepository, eventRepository);
        start = LocalDateTime.now().minusMinutes(1).withNano(0);
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    @Test
    public void eventCanStartAndEndOnlyThroughValidLifecycle() throws Exception {
        FlashSaleEvent event = event("EVT-1", 90);
        controller.createEvent(event);
        assertEquals(SaleStatus.UPCOMING, event.getSaleStatus());

        controller.startEvent("EVT-1", start.plusMinutes(1));
        assertEquals(SaleStatus.ACTIVE, controller.listEvents().get(0).getSaleStatus());
        controller.endEvent("EVT-1");
        assertEquals(SaleStatus.ENDED, controller.listEvents().get(0).getSaleStatus());

        try {
            controller.endEvent("EVT-1");
            fail("Sự kiện đã kết thúc không được kết thúc lần nữa");
        } catch (InvalidEventException expected) {
            assertTrue(expected.getMessage().contains("Không thể kết thúc"));
        }
    }

    @Test
    public void durationBoundariesOneAndTwoHoursAreAccepted() throws Exception {
        controller.createEvent(event("EVT-60", 60));
        controller.createEvent(event("EVT-120", 120));
        assertEquals(2, controller.listEvents().size());
    }

    @Test
    public void durationOutsideOneToTwoHoursIsRejected() throws Exception {
        assertInvalidDuration(event("EVT-59", 59));
        assertInvalidDuration(event("EVT-121", 121));
    }

    @Test
    public void discountBoundariesThirtyAndSeventyPercentAreAccepted() throws Exception {
        controller.addFlashSaleItem(item("FI-30", 1_000, 700));
        controller.addFlashSaleItem(item("FI-70", 1_000, 300));
        assertEquals(2, controller.getFlashSaleItems(10).size());
    }

    @Test
    public void discountOutsideThirtyToSeventyPercentIsRejected() throws Exception {
        assertInvalidDiscount(item("FI-LOW", 1_000, 701));
        assertInvalidDiscount(item("FI-HIGH", 1_000, 299));
    }

    private void assertInvalidDuration(FlashSaleEvent event) throws Exception {
        try {
            controller.createEvent(event);
            fail("Thời lượng ngoài 1-2 giờ phải bị chặn");
        } catch (InvalidEventException expected) {
            assertTrue(expected.getMessage().contains("1 đến 2 giờ"));
        }
    }

    private void assertInvalidDiscount(FlashItem item) throws Exception {
        try {
            controller.addFlashSaleItem(item);
            fail("Giảm giá ngoài 30-70% phải bị chặn");
        } catch (InvalidDiscountException expected) {
            assertTrue(expected.getMessage().contains("30% đến 70%"));
        }
    }

    private FlashSaleEvent event(String id, long durationMinutes) {
        return new FlashSaleEvent(id, "Event " + id,
                start.format(FlashSaleFormats.EVENT_TIME),
                start.plusMinutes(durationMinutes).format(FlashSaleFormats.EVENT_TIME),
                SaleStatus.UPCOMING.name());
    }

    private static FlashItem item(String id, int originalPrice, int salePrice) {
        return new FlashItem(id, "P-1", "EVT-1", "Product",
                originalPrice, salePrice, 10);
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
