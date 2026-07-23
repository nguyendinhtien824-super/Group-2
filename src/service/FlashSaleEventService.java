package service;

import config.FlashSaleFormats;
import exception.InvalidEventException;
import model.FlashSaleEvent;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FlashSaleEventService {
    private final FlashSaleEventRepository eventRepository;

    public FlashSaleEventService(FlashSaleEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public FlashSaleEvent createEvent(FlashSaleEvent event) throws InvalidEventException {
        validateIdentity(event);
        FlashSalePolicy.validateEventSchedule(event);
        if (eventRepository.findById(event.getEventId()) != null) {
            throw new InvalidEventException("Mã sự kiện đã tồn tại: " + event.getEventId());
        }
        event.setStatus(SaleStatus.UPCOMING);
        event.setUnlockTime("");
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent startEvent(String eventId) throws InvalidEventException {
        return startEvent(eventId, LocalDateTime.now());
    }

    public FlashSaleEvent startEvent(String eventId, LocalDateTime now) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        FlashSalePolicy.validateEventSchedule(event);
        LocalDateTime start = parseTime(event.getStartTime(), "bắt đầu");
        LocalDateTime end = parseTime(event.getEndTime(), "kết thúc");
        if (now.isBefore(start)) {
            throw new InvalidEventException("Sự kiện Flash Sale chưa đến thời điểm bắt đầu");
        }
        if (!now.isBefore(end)) {
            throw new InvalidEventException("Sự kiện Flash Sale đã quá thời điểm kết thúc");
        }
        event.start();
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent endEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        event.end();
        eventRepository.save(event);
        return event;
    }

    public List<FlashSaleEvent> listEvents() {
        return eventRepository.findAll();
    }

    public FlashSaleEvent getEvent(String eventId) throws InvalidEventException {
        return requireEvent(eventId);
    }

    public FlashSaleEvent updateEvent(FlashSaleEvent update) throws InvalidEventException {
        validateIdentity(update);
        FlashSaleEvent current = requireEvent(update.getEventId());
        if (current.getSaleStatus() == SaleStatus.ACTIVE
                || current.getSaleStatus() == SaleStatus.LOCKED
                || current.getSaleStatus().isEnded()) {
            throw new InvalidEventException(
                    "Chỉ được sửa sự kiện ở trạng thái UPCOMING hoặc INACTIVE");
        }
        FlashSalePolicy.validateEventSchedule(update);
        current.setName(update.getName());
        current.setStartTime(update.getStartTime());
        current.setEndTime(update.getEndTime());
        eventRepository.save(current);
        return current;
    }

    public boolean deleteEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getSaleStatus() == SaleStatus.ACTIVE
                || event.getSaleStatus() == SaleStatus.LOCKED) {
            throw new InvalidEventException("Không thể xóa sự kiện đang hoạt động hoặc đang khóa");
        }
        return eventRepository.deleteById(event.getEventId());
    }

    public FlashSaleEvent lockEvent(String eventId, LocalDateTime unlockAt)
            throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getSaleStatus() != SaleStatus.ACTIVE) {
            throw new InvalidEventException("Chỉ sự kiện ACTIVE mới có thể tạm khóa");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = parseTime(event.getEndTime(), "kết thúc");
        if (unlockAt == null || !unlockAt.isAfter(now) || unlockAt.isAfter(end)) {
            throw new InvalidEventException("Thời điểm mở khóa phải ở tương lai và không sau giờ kết thúc");
        }
        event.setStatus(SaleStatus.LOCKED);
        event.setUnlockTime(unlockAt.format(config.FlashSaleFormats.EVENT_TIME));
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent unlockEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getSaleStatus() != SaleStatus.LOCKED) {
            throw new InvalidEventException("Sự kiện không ở trạng thái LOCKED");
        }
        LocalDateTime end = parseTime(event.getEndTime(), "kết thúc");
        if (!LocalDateTime.now().isBefore(end)) {
            event.setStatus(SaleStatus.ENDED);
            event.setUnlockTime("");
            eventRepository.save(event);
            throw new InvalidEventException("Sự kiện đã kết thúc nên không thể mở lại");
        }
        event.setStatus(SaleStatus.ACTIVE);
        event.setUnlockTime("");
        eventRepository.save(event);
        return event;
    }

    private FlashSaleEvent requireEvent(String eventId) throws InvalidEventException {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidEventException("Mã sự kiện không được để trống");
        }
        FlashSaleEvent event = eventRepository.findById(eventId.trim());
        if (event == null) {
            throw new InvalidEventException("Sự kiện Flash Sale không tồn tại: " + eventId);
        }
        return event;
    }

    private static void validateIdentity(FlashSaleEvent event) throws InvalidEventException {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new InvalidEventException("Mã sự kiện không được để trống");
        }
        if (event.getName() == null || event.getName().isBlank()) {
            throw new InvalidEventException("Tên sự kiện không được để trống");
        }
    }

    private static LocalDateTime parseTime(String value, String field) throws InvalidEventException {
        try {
            return LocalDateTime.parse(value, FlashSaleFormats.EVENT_TIME);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidEventException("Thời gian " + field + " không hợp lệ", e);
        }
    }
}
