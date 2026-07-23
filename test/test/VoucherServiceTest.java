package test;

import controller.VoucherController;
import model.Voucher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.VoucherRepository;
import service.VoucherService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class VoucherServiceTest {
    private Path testDirectory;
    private VoucherRepository repository;
    private VoucherService service;
    private VoucherController controller;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("voucher-admin-");
        repository = new VoucherRepository(testDirectory.toString());
        service = new VoucherService(repository);
        controller = new VoucherController(service);
    }

    @After
    public void tearDown() throws IOException {
        if (testDirectory == null || !Files.exists(testDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(testDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    public void create_NormalizesCodeAndFixedDiscountCap() {
        Voucher voucher = service.create("sale50", "fixed", 50_000,
                999_999, 100_000, 10);

        assertEquals("SALE50", voucher.getCode());
        assertEquals("FIXED", voucher.getType());
        assertEquals(50_000, voucher.getMaxDiscount());
        assertEquals(1, service.search("sale").size());
    }

    @Test
    public void create_RejectsDuplicateAndInvalidRanges() {
        service.create("SAVE10", "PERCENTAGE", 10, 50_000, 100_000, 5);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("save10", "FIXED", 10_000, 0, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("OVER100", "PERCENTAGE", 101, 50_000, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("NEGUSES", "FIXED", 10_000, 0, 0, -1));
    }

    @Test
    public void update_ValidatesUniquenessAndAllowsExhaustedVoucher() {
        Voucher first = service.create("FIRST10", "PERCENTAGE", 10, 50_000, 0, 5);
        service.create("SECOND", "FIXED", 20_000, 20_000, 0, 5);

        Voucher updated = service.update(first.getVoucherId(), "FIRST20", null,
                20, 60_000, null, 0);

        assertEquals("FIRST20", updated.getCode());
        assertEquals(0, updated.getRemainingUses());
        assertThrows(IllegalArgumentException.class,
                () -> service.update(first.getVoucherId(), "SECOND", null,
                        null, null, null, null));
    }

    @Test
    public void controller_ReturnsFriendlyFailureAndDeleteRemovesVoucher() {
        VoucherController.Result<Voucher> invalid = controller.create(
                "x", "UNKNOWN", 0, 0, -1, -1);
        assertFalse(invalid.success());

        Voucher voucher = service.create("DELETE10", "PERCENTAGE", 10, 10_000, 0, 1);
        VoucherController.Result<Voucher> deleted = controller.delete("delete10");

        assertTrue(deleted.success());
        assertNull(repository.findById(voucher.getVoucherId()));
    }
}
