package model;

public class Order extends BaseEntity {
    private String orderId;
    private String customerId;
    private String orderDate;
    private int totalAmount;
    private String status;

    public Order() {}

    public Order(String orderId, String customerId, String orderDate, int totalAmount, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    @Override
    public String getId() {
        return orderId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", orderId, customerId, orderDate, String.valueOf(totalAmount), status);
    }

    public static Order fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        return new Order(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            Integer.parseInt(parts[3].trim()),
            parts[4].trim()
        );
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    
    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

