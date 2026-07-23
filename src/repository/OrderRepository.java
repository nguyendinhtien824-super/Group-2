package repository;

import model.Order;
import model.enums.OrderStatus;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Repository xử lý I/O dữ liệu đơn hàng từ file CSV.
 */
public class OrderRepository extends CsvRepository<Order> {
    public OrderRepository() {
        super(Order.class, "orders.csv",
                "orderId,customerId,customerName,orderDate,totalAmount,status,eventId");
    }

    public OrderRepository(String dataDirectory) {
        super(Order.class, dataDirectory, "orders.csv",
                "orderId,customerId,customerName,orderDate,totalAmount,status,eventId");
    }

    /**
     * Tự động sinh ID đơn hàng mới dạng O-XXXXXX dựa vào ID lớn nhất hiện tại.
     */
    public String generateNewOrderId() {
        int maxId = findAll().stream()
                .map(Order::getOrderId)
                .filter(id -> id != null && id.startsWith("O-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("O-%05d", maxId + 1);
    }

    public List<Order> findPurchasesCountingTowardLimit(String customerId, String eventId) {
        Set<OrderStatus> countedStatuses = EnumSet.of(
                OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.SUCCESS);
        return findAll().stream()
                .filter(order -> customerId.equals(order.getCustomerId()))
                .filter(order -> eventId.equals(order.getEventId()))
                .filter(order -> countedStatuses.contains(order.getOrderStatus()))
                .toList();
    }
}
