package view;

import controller.OrderTrackingController;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Customer order and transaction tables. */
public class OrderHistoryView {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final OrderTrackingController controller;

    public OrderHistoryView(OrderTrackingController controller) {
        this.controller = controller;
    }

    public void displayOrders(String customerId) {
        List<Order> orders = controller.getOrdersByCustomer(customerId);
        if (orders.isEmpty()) {
            System.out.println("Bạn chưa có đơn hàng nào.");
            return;
        }

        System.out.println("\n--- DANH SÁCH ĐƠN HÀNG ---");
        System.out.printf("%-14s | %-30s | %-19s | %15s | %-12s%n",
                "Mã đơn", "Sản phẩm", "Ngày đặt", "Tổng tiền", "Trạng thái");
        System.out.println("-".repeat(105));
        for (Order order : orders) {
            System.out.printf("%-14s | %-30s | %-19s | %,15d | %-12s%n",
                    order.getOrderId(), productSummary(order.getOrderId()),
                    order.getOrderDate(), order.getTotalAmount(), order.getStatus());
        }
        long successCount = orders.stream()
                .filter(order -> "SUCCESS".equalsIgnoreCase(order.getStatus()))
                .count();
        long totalSpent = orders.stream()
                .filter(order -> "SUCCESS".equalsIgnoreCase(order.getStatus()))
                .mapToLong(Order::getTotalAmount)
                .sum();
        System.out.printf("Tổng: %d đơn | Thành công: %d | Đã chi: %,d VND%n",
                orders.size(), successCount, totalSpent);
    }

    public void displayTransactions(String customerId) {
        List<OrderTransaction> transactions = controller.getTransactionsByCustomer(customerId);
        if (transactions.isEmpty()) {
            System.out.println("Bạn chưa có giao dịch nào.");
            return;
        }

        System.out.println("\n--- LỊCH SỬ GIAO DỊCH ---");
        System.out.printf("%-16s | %-14s | %-14s | %4s | %-10s | %-19s | %s%n",
                "Mã giao dịch", "Mã đơn", "Mã item", "SL", "Trạng thái", "Thời gian", "Ghi chú");
        System.out.println("-".repeat(120));
        for (OrderTransaction transaction : transactions) {
            System.out.printf("%-16s | %-14s | %-14s | %4d | %-10s | %-19s | %s%n",
                    transaction.getTransactionId(), transaction.getOrderId(),
                    transaction.getItemId(), transaction.getQuantity(),
                    transaction.getStatus(), formatTimestamp(transaction.getTimestamp()),
                    transaction.getMessage());
        }
    }

    private String productSummary(String orderId) {
        List<OrderDetail> details = controller.getOrderDetails(orderId);
        if (details.isEmpty()) {
            return "Không có chi tiết";
        }
        String name = controller.getProductName(details.get(0).getProductId());
        if (details.size() > 1) {
            name += " +" + (details.size() - 1);
        }
        return trim(name, 30);
    }

    private static String formatTimestamp(long timestamp) {
        return timestamp <= 0 ? "N/A" : TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    private static String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
