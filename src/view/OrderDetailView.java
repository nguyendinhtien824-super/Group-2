package view;

import controller.OrderTrackingController;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;

import java.util.List;

/** Customer-owned order invoice and its transaction trail. */
public class OrderDetailView {
    private final OrderTrackingController controller;
    private final ConsoleInput input;

    public OrderDetailView(OrderTrackingController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display(String customerId) {
        String orderId = input.readStringRequired("Mã đơn hàng");
        Order order = controller.getOrderById(orderId);
        if (order == null) {
            System.out.println("Không tìm thấy đơn hàng: " + orderId);
            return;
        }
        if (!customerId.equals(order.getCustomerId())) {
            System.out.println("Bạn không có quyền xem đơn hàng này.");
            return;
        }

        printHeader(order);
        printItems(controller.getOrderDetails(orderId));
        printTransactions(controller.getTransactionsByOrder(orderId));
    }

    private void printHeader(Order order) {
        System.out.println("\n--- CHI TIẾT ĐƠN HÀNG ---");
        System.out.println("Mã đơn: " + order.getOrderId());
        System.out.println("Sự kiện: " + blankAsUnknown(order.getEventId()));
        System.out.println("Khách hàng: " + order.getCustomerName());
        System.out.println("Ngày đặt: " + order.getOrderDate());
        System.out.println("Trạng thái: " + order.getStatus());
        System.out.printf("Tổng thanh toán: %,d VND%n", order.getTotalAmount());
    }

    private void printItems(List<OrderDetail> details) {
        if (details.isEmpty()) {
            System.out.println("Đơn hàng chưa có chi tiết sản phẩm.");
            return;
        }
        System.out.printf("%n%-14s | %-30s | %4s | %14s | %15s%n",
                "Mã sản phẩm", "Tên sản phẩm", "SL", "Đơn giá", "Thành tiền");
        System.out.println("-".repeat(91));
        for (OrderDetail detail : details) {
            System.out.printf("%-14s | %-30s | %4d | %,14d | %,15d%n",
                    detail.getProductId(), trim(controller.getProductName(detail.getProductId()), 30),
                    detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal());
        }
    }

    private void printTransactions(List<OrderTransaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        System.out.println("\nLịch sử xử lý:");
        for (OrderTransaction transaction : transactions) {
            System.out.printf("- %s | %s | %s%n", transaction.getTransactionId(),
                    transaction.getStatus(), transaction.getMessage());
        }
    }

    private static String blankAsUnknown(String value) {
        return value == null || value.isBlank() ? "Không xác định" : value;
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
