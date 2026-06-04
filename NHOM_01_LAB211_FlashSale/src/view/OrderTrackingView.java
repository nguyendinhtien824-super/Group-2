package view;

import controller.OrderTrackingController;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Giao diện console theo dõi đơn hàng dành cho khách hàng đã đăng nhập.
 */
public class OrderTrackingView {

    private final OrderTrackingController controller;
    private final ConsoleInput input;

    public OrderTrackingView(OrderTrackingController controller) {
        this.controller = controller;
        this.input = new ConsoleInput(new Scanner(System.in));
    }

    /**
     * Hiển thị menu theo dõi đơn hàng cho khách hàng đang đăng nhập.
     *
     * @param customerId ID khách hàng đang đăng nhập
     */
    public void display(String customerId) {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("===== THEO DOI DON HANG CUA TOI =====");
            System.out.println("1. Danh sach tat ca don hang cua toi");
            System.out.println("2. Xem chi tiet mot don hang");
            System.out.println("3. Lich su giao dich cua toi");
            System.out.println("0. Quay lai menu chinh");

            int choice = input.readInt("Nhap lua chon", 0);
            switch (choice) {
                case 1:
                    showMyOrders(customerId);
                    break;
                case 2:
                    showOrderDetails(customerId);
                    break;
                case 3:
                    showMyTransactions(customerId);
                    break;
                case 0:
                    back = true;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Danh sách đơn hàng
    // ─────────────────────────────────────────────────────────────────────────

    private void showMyOrders(String customerId) {
        System.out.println("\n--- DANH SACH DON HANG CUA TOI ---");
        List<Order> orders = controller.getOrdersByCustomer(customerId);

        if (orders.isEmpty()) {
            System.out.println("Ban chua co don hang nao.");
            return;
        }

        System.out.printf("%-12s | %-22s | %-14s | %-10s%n",
                "Ma don hang", "Ngay dat", "Tong tien (VND)", "Trang thai");
        System.out.println("-----------------------------------------------------------------------");

        for (Order o : orders) {
            System.out.printf("%-12s | %-22s | %,14d | %-10s%n",
                    o.getOrderId(),
                    o.getOrderDate(),
                    o.getTotalAmount(),
                    formatStatus(o.getStatus()));
        }

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Tong cong: " + orders.size() + " don hang.");

        // Thống kê nhanh
        long successCount = orders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getStatus())).count();
        long totalSpent = orders.stream()
                .filter(o -> "SUCCESS".equalsIgnoreCase(o.getStatus()))
                .mapToLong(Order::getTotalAmount).sum();
        System.out.printf("  Don hang thanh cong : %d%n", successCount);
        System.out.printf("  Tong tien da chi    : %,d VND%n", totalSpent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Chi tiết đơn hàng
    // ─────────────────────────────────────────────────────────────────────────

    private void showOrderDetails(String customerId) {
        System.out.println("\n--- CHI TIET DON HANG ---");
        String orderId = input.readLine("Nhap ma don hang (VD: ORD-00001): ");

        // Kiểm tra đơn hàng tồn tại
        Order order = controller.getOrderById(orderId);
        if (order == null) {
            System.out.println("Khong tim thay don hang: " + orderId);
            return;
        }

        // Kiểm tra đơn hàng thuộc về khách hàng đang đăng nhập
        if (!customerId.equals(order.getCustomerId())) {
            System.out.println("Don hang nay khong thuoc ve tai khoan cua ban.");
            return;
        }

        // Thông tin chung
        System.out.println();
        System.out.println("  Ma don hang  : " + order.getOrderId());
        System.out.println("  Ngay dat     : " + order.getOrderDate());
        System.out.println("  Trang thai   : " + formatStatus(order.getStatus()));
        System.out.printf ("  Tong tien    : %,d VND%n", order.getTotalAmount());

        // Chi tiết sản phẩm
        List<OrderDetail> details = controller.getOrderDetails(orderId);
        if (details.isEmpty()) {
            System.out.println("  (Khong co chi tiet san pham nao.)");
        } else {
            System.out.println();
            System.out.printf("  %-14s | %-20s | %-8s | %-14s | %-14s%n",
                    "Ma chi tiet", "Ma san pham", "So luong", "Don gia (VND)", "Thanh tien (VND)");
            System.out.println("  " + "-".repeat(82));
            for (OrderDetail d : details) {
                System.out.printf("  %-14s | %-20s | %8d | %,14d | %,14d%n",
                        d.getDetailId(),
                        d.getProductId(),
                        d.getQuantity(),
                        d.getUnitPrice(),
                        d.getSubtotal());
            }
            System.out.println("  " + "-".repeat(82));
        }

        // Lịch sử giao dịch của đơn hàng này
        List<OrderTransaction> txns = controller.getTransactionsByOrder(orderId);
        if (!txns.isEmpty()) {
            System.out.println();
            System.out.println("  --- Giao dich lien quan ---");
            for (OrderTransaction t : txns) {
                System.out.printf("  [%s] %s  |  Item: %-12s  |  SL: %d  |  %s%n",
                        formatStatus(t.getStatus()),
                        formatTimestamp(t.getTimestamp()),
                        t.getItemId(),
                        t.getQuantity(),
                        t.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Lịch sử giao dịch
    // ─────────────────────────────────────────────────────────────────────────

    private void showMyTransactions(String customerId) {
        System.out.println("\n--- LICH SU GIAO DICH CUA TOI ---");
        List<OrderTransaction> txns = controller.getTransactionsByCustomer(customerId);

        if (txns.isEmpty()) {
            System.out.println("Ban chua co giao dich nao.");
            return;
        }

        System.out.printf("%-16s | %-12s | %-14s | %-5s | %-10s | %-24s | %s%n",
                "Ma giao dich", "Ma don hang", "Ma san pham", "SL", "Trang thai", "Thoi gian", "Ghi chu");
        System.out.println("-".repeat(115));

        for (OrderTransaction t : txns) {
            System.out.printf("%-16s | %-12s | %-14s | %5d | %-10s | %-24s | %s%n",
                    t.getTransactionId(),
                    t.getOrderId(),
                    t.getItemId(),
                    t.getQuantity(),
                    formatStatus(t.getStatus()),
                    formatTimestamp(t.getTimestamp()),
                    t.getMessage());
        }

        System.out.println("-".repeat(115));
        System.out.println("Tong cong: " + txns.size() + " giao dich.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String formatStatus(String status) {
        if (status == null) return "UNKNOWN";
        switch (status.toUpperCase()) {
            case "SUCCESS": return "THANH CONG";
            case "FAILED":  return "THAT BAI";
            case "PENDING": return "CHO XU LY";
            default:        return status;
        }
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "N/A";
        try {
            Date date = new Date(timestamp);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
