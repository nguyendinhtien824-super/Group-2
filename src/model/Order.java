package model;

import exception.InvalidOrderStateException;
import model.enums.OrderStatus;

public class Order extends BaseEntity {
    private String orderId;
    private String customerId;
    private String customerName;
    private String orderDate;
    private int totalAmount;
    private OrderStatus status = OrderStatus.PENDING;
    private String eventId;

    public Order() {}

    public Order(String orderId, String customerId, String customerName, String orderDate, int totalAmount, String status) {
        this(orderId, customerId, customerName, orderDate, totalAmount,
                OrderStatus.fromValue(status), "");
    }

    public Order(String orderId, String customerId, String customerName, String orderDate,
                 int totalAmount, OrderStatus status, String eventId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.eventId = eventId != null ? eventId : "";
    }

    @Override
    public String getId() {
        return orderId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", orderId, customerId, customerName, orderDate,
                String.valueOf(totalAmount), getStatus(), eventId != null ? eventId : "");
    }

    public static Order fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) return null;
        Order parsed = new Order(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            Integer.parseInt(parts[4].trim()),
            parts[5].trim()
        );
        if (parts.length >= 7) {
            parsed.setEventId(parts[6].trim());
        }
        return parsed;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    
    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status.name(); }
    public OrderStatus getOrderStatus() { return status; }
    public void setStatus(String status) { this.status = OrderStatus.fromValue(status); }
    public void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không được để trống");
        }
        this.status = status;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId != null ? eventId : ""; }

    public void approve() throws InvalidOrderStateException {
        requireStatus(OrderStatus.PENDING, "duyệt");
        status = OrderStatus.APPROVED;
    }

    public void complete() throws InvalidOrderStateException {
        if (status != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                    "Chỉ đơn hàng ở trạng thái APPROVED mới có thể chuyển sang SUCCESS. "
                            + "Trạng thái hiện tại: " + status);
        }
        status = OrderStatus.SUCCESS;
    }

    public void cancel() throws InvalidOrderStateException {
        if (status != OrderStatus.PENDING && status != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                    "Chỉ đơn hàng PENDING hoặc APPROVED mới được hủy. Trạng thái hiện tại: " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus expected, String action) throws InvalidOrderStateException {
        if (status != expected) {
            throw new InvalidOrderStateException(
                    "Chỉ đơn hàng ở trạng thái " + expected + " mới được " + action
                            + ". Trạng thái hiện tại: " + status);
        }
    }
}

