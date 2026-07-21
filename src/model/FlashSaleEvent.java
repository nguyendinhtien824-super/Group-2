package model;

import exception.InvalidEventException;
import model.enums.SaleStatus;

public class FlashSaleEvent extends BaseEntity {
    private String eventId;
    private String name;
    private String startTime;
    private String endTime;
    private SaleStatus status = SaleStatus.UPCOMING;
    private String unlockTime;

    public FlashSaleEvent() {}

    public FlashSaleEvent(String eventId, String name, String startTime, String endTime, String status) {
        this(eventId, name, startTime, endTime, status, "");
    }

    public FlashSaleEvent(String eventId, String name, String startTime, String endTime, String status, String unlockTime) {
        this.eventId = eventId;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SaleStatus.fromValue(status);
        this.unlockTime = unlockTime;
    }

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", eventId, name, startTime, endTime, getStatus(),
                unlockTime != null ? unlockTime : "");
    }

    public static FlashSaleEvent fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        String status = parts[4].trim();
        String unlockTime = (parts.length >= 6) ? parts[5].trim() : "";
        return new FlashSaleEvent(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            status,
            unlockTime
        );
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getStatus() { return status.name(); }
    public SaleStatus getSaleStatus() { return status; }
    public void setStatus(String status) { this.status = SaleStatus.fromValue(status); }
    public void setStatus(SaleStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái sự kiện không được để trống");
        }
        this.status = status;
    }

    public void start() throws InvalidEventException {
        if (!status.canStart()) {
            throw new InvalidEventException("Không thể bắt đầu sự kiện ở trạng thái " + status);
        }
        status = SaleStatus.ACTIVE;
    }

    public void end() throws InvalidEventException {
        if (!status.canEnd()) {
            throw new InvalidEventException("Không thể kết thúc sự kiện ở trạng thái " + status);
        }
        status = SaleStatus.ENDED;
        unlockTime = "";
    }

    public String getUnlockTime() { return unlockTime; }
    public void setUnlockTime(String unlockTime) { this.unlockTime = unlockTime; }
}

