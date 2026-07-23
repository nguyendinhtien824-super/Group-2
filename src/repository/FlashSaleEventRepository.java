package repository;

import config.FlashSaleFormats;
import model.FlashSaleEvent;
import model.enums.SaleStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Repository xử lý I/O dữ liệu sự kiện Flash Sale từ file CSV.
 */
public class FlashSaleEventRepository extends CsvRepository<FlashSaleEvent> {
    public FlashSaleEventRepository() {
        super(FlashSaleEvent.class, "flash_events.csv", "eventId,name,startTime,endTime,status,unlockTime");
        syncEventStatuses();
    }

    public FlashSaleEventRepository(String dataDirectory) {
        super(FlashSaleEvent.class, dataDirectory, "flash_events.csv", "eventId,name,startTime,endTime,status,unlockTime");
        syncEventStatuses();
    }

    private final ThreadLocal<Boolean> isSyncing = ThreadLocal.withInitial(() -> false);

    @Override
    public List<FlashSaleEvent> findAll() {
        if (!isSyncing.get()) {
            isSyncing.set(true);
            try {
                syncEventStatuses();
            } finally {
                isSyncing.set(false);
            }
        }
        return super.findAll();
    }

    public void syncEventStatuses() {
        isSyncing.set(true);
        try {
            LocalDateTime now = LocalDateTime.now();
            List<FlashSaleEvent> allEvents = super.findAll();
            boolean changed = false;
            for (FlashSaleEvent event : allEvents) {
                if (event.getSaleStatus() == SaleStatus.LOCKED
                        && event.getUnlockTime() != null && !event.getUnlockTime().isBlank()) {
                    LocalDateTime unlock = parseTime(event.getUnlockTime(), event.getEventId());
                    if (!now.isBefore(unlock)) {
                        event.setStatus(SaleStatus.ACTIVE);
                        event.setUnlockTime("");
                        changed = true;
                    }
                }
                if (event.getSaleStatus() == SaleStatus.ACTIVE) {
                    LocalDateTime end = parseTime(event.getEndTime(), event.getEventId());
                    if (!now.isBefore(end)) {
                        event.setStatus(SaleStatus.ENDED);
                        changed = true;
                    }
                }
            }
            if (changed) {
                super.saveAll(allEvents);
            }
        } finally {
            isSyncing.set(false);
        }
    }

    private static LocalDateTime parseTime(String value, String eventId) {
        try {
            return LocalDateTime.parse(value, FlashSaleFormats.EVENT_TIME);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalStateException("Thời gian không hợp lệ của sự kiện " + eventId, e);
        }
    }
}
