package model;

public class FlashSaleEvent extends BaseEntity {
    private String eventId;
    private String name;
    private String startTime;
    private String endTime;
    private String status;

    public FlashSaleEvent() {}

    public FlashSaleEvent(String eventId, String name, String startTime, String endTime, String status) {
        this.eventId = eventId;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", eventId, name, startTime, endTime, status);
    }

    public static FlashSaleEvent fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        return new FlashSaleEvent(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim()
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
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

