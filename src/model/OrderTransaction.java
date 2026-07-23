package model;

public class OrderTransaction extends BaseEntity {
    private String transactionId;
    private String orderId;
    private String customerId;
    private String itemId;
    private int quantity;
    private String status;
    private String message;
    private long timestamp;

    public OrderTransaction() {}

    public OrderTransaction(String transactionId, String orderId, String customerId, String itemId, int quantity, String status, String message, long timestamp) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return transactionId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", transactionId, orderId, customerId, itemId, String.valueOf(quantity), status, message, String.valueOf(timestamp));
    }

    public static OrderTransaction fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 8) return null;
        return new OrderTransaction(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            Integer.parseInt(parts[4].trim()),
            parts[5].trim(),
            parts[6].trim(),
            Long.parseLong(parts[7].trim())
        );
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

