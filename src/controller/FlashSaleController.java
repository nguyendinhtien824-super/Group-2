package controller;

import exception.InvalidDiscountException;
import exception.InvalidEventException;
import model.FlashItem;
import model.FlashSaleEvent;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashItemRepository;
import service.FlashSaleEventService;
import service.FlashSalePolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class FlashSaleController {

    private final FlashItemRepository flashItemRepository;
    private final FlashSaleEventService eventService;

    public FlashSaleController(FlashItemRepository flashItemRepository) {
        this(flashItemRepository, (FlashSaleEventService) null);
    }

    public FlashSaleController(FlashItemRepository flashItemRepository,
                               FlashSaleEventRepository eventRepository) {
        this(flashItemRepository, new FlashSaleEventService(eventRepository));
    }

    public FlashSaleController(FlashItemRepository flashItemRepository,
                               FlashSaleEventService eventService) {
        this.flashItemRepository = flashItemRepository;
        this.eventService = eventService;
    }

    public List<FlashItem> getFlashSaleItems(int limit) {
        return flashItemRepository.findAll().stream()
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    public FlashItem addFlashSaleItem(FlashItem item) throws InvalidDiscountException {
        FlashSalePolicy.validateDiscount(item);
        if (flashItemRepository.findById(item.getItemId()) != null) {
            throw new IllegalArgumentException("Mã Flash Sale item đã tồn tại: " + item.getItemId());
        }
        flashItemRepository.save(item);
        return item;
    }

    public FlashItem findFlashSaleItem(String itemId) {
        return flashItemRepository.findById(itemId);
    }

    public List<FlashItem> getItemsByEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return List.of();
        }
        return flashItemRepository.findAll().stream()
                .filter(item -> eventId.equalsIgnoreCase(item.getEventId()))
                .toList();
    }

    public boolean updateFlashSaleItem(FlashItem item, int expectedVersion)
            throws InvalidDiscountException {
        FlashSalePolicy.validateDiscount(item);
        return flashItemRepository.updateItem(item, expectedVersion);
    }

    public boolean deleteFlashSaleItem(String itemId) {
        return flashItemRepository.deleteById(itemId);
    }

    public FlashSaleEvent createEvent(FlashSaleEvent event) throws InvalidEventException {
        return requireEventService().createEvent(event);
    }

    public FlashSaleEvent startEvent(String eventId) throws InvalidEventException {
        return requireEventService().startEvent(eventId);
    }

    public FlashSaleEvent startEvent(String eventId, LocalDateTime now) throws InvalidEventException {
        return requireEventService().startEvent(eventId, now);
    }

    public FlashSaleEvent endEvent(String eventId) throws InvalidEventException {
        return requireEventService().endEvent(eventId);
    }

    public List<FlashSaleEvent> listEvents() {
        return requireEventService().listEvents();
    }

    public List<FlashSaleEvent> listCustomerVisibleEvents() {
        return listEvents().stream()
                .filter(event -> event.getSaleStatus() == SaleStatus.ACTIVE
                        || event.getSaleStatus() == SaleStatus.LOCKED)
                .toList();
    }

    public FlashSaleEvent findEvent(String eventId) throws InvalidEventException {
        return requireEventService().getEvent(eventId);
    }

    public FlashSaleEvent updateEvent(FlashSaleEvent event) throws InvalidEventException {
        return requireEventService().updateEvent(event);
    }

    public boolean deleteEvent(String eventId) throws InvalidEventException {
        if (!getItemsByEvent(eventId).isEmpty()) {
            throw new InvalidEventException(
                    "Hãy xóa toàn bộ Flash Sale item trước khi xóa sự kiện");
        }
        return requireEventService().deleteEvent(eventId);
    }

    public FlashSaleEvent lockEvent(String eventId, LocalDateTime unlockAt)
            throws InvalidEventException {
        return requireEventService().lockEvent(eventId, unlockAt);
    }

    public FlashSaleEvent unlockEvent(String eventId) throws InvalidEventException {
        return requireEventService().unlockEvent(eventId);
    }

    private FlashSaleEventService requireEventService() {
        if (eventService == null) {
            throw new IllegalStateException("FlashSaleEventService chưa được cấu hình");
        }
        return eventService;
    }
}


// Member 3
