package test;

import controller.FlashSaleController;
import exception.InvalidEventException;
import model.FlashItem;
import model.FlashSaleEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import config.FlashSaleFormats;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FlashSaleAdminCrudTest {
    private static final Path TEST_DIR = Path.of("test_data", "flash_admin_crud");
    private FlashSaleController controller;

    @Before
    public void setUp() throws Exception {
        deleteDirectory();
        Files.createDirectories(TEST_DIR);
        controller = new FlashSaleController(
                new FlashItemRepository(TEST_DIR.toString()),
                new FlashSaleEventRepository(TEST_DIR.toString()));
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory();
    }

    @Test
    public void eventAndItemCrudPreserveLifecycleAndVersion() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime end = start.plusMinutes(90);
        FlashSaleEvent event = new FlashSaleEvent("EV-CRUD", "Sự kiện CRUD",
                format(start), format(end), "UPCOMING");

        controller.createEvent(event);
        event.setName("Sự kiện đã sửa");
        controller.updateEvent(event);
        assertEquals("Sự kiện đã sửa", controller.findEvent("EV-CRUD").getName());

        FlashItem item = new FlashItem("FI-CRUD", "P-00001", "EV-CRUD",
                "Sản phẩm", 1_000_000, 500_000, 10);
        controller.addFlashSaleItem(item);
        int version = item.getVersion();
        item.setSalePrice(450_000);
        assertTrue(controller.updateFlashSaleItem(item, version));
        assertEquals(version + 1, controller.findFlashSaleItem("FI-CRUD").getVersion());

        assertThrows(InvalidEventException.class, () -> controller.deleteEvent("EV-CRUD"));
        assertTrue(controller.deleteFlashSaleItem("FI-CRUD"));

        controller.startEvent("EV-CRUD", start.plusMinutes(1));
        controller.lockEvent("EV-CRUD", LocalDateTime.now().plusMinutes(10));
        controller.unlockEvent("EV-CRUD");
        controller.endEvent("EV-CRUD");
        assertTrue(controller.deleteEvent("EV-CRUD"));
    }

    @Test
    public void repositoryRejectsInventoryInvariantViolation() {
        FlashItem invalid = new FlashItem("FI-BAD", "P-1", "EV-1",
                "Bad", 100_000, 50_000, 1);
        invalid.setSoldQty(2);

        assertThrows(IllegalArgumentException.class,
                () -> new FlashItemRepository(TEST_DIR.toString()).save(invalid));
        assertFalse(Files.exists(TEST_DIR.resolve("flash_items.csv.tmp")));
    }

    private static String format(LocalDateTime value) {
        return value.format(FlashSaleFormats.EVENT_TIME);
    }

    private static void deleteDirectory() throws Exception {
        if (!Files.exists(TEST_DIR)) {
            return;
        }
        try (var paths = Files.walk(TEST_DIR)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
