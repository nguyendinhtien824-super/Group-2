package view;

import controller.AdminOrderController;
import model.Order;
import model.OrderDetail;

import java.util.List;

/** Admin order UI; state-machine checks remain in the service layer. */
public class AdminOrderView {
    private static final int DISPLAY_LIMIT = 20;

    private final AdminOrderController controller;
    private final ConsoleInput input;

    public AdminOrderView(AdminOrderController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- QUẢN LÝ ĐƠN HÀNG ---");
            System.out.println("1. Danh sách đơn hàng");
            System.out.println("2. Xem và xử lý một đơn");
            System.out.println("0. Quay lại");
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> displayOrders(controller.listOrders());
                    case 2 -> manageOrder();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác đơn hàng.");
            }
        }
    }

    private void manageOrder() {
        String orderId = input.readStringRequired("Mã đơn hàng");
        Order order = controller.findOrder(orderId);
        if (order == null) {
            System.out.println("Không tìm thấy đơn hàng.");
            return;
        }
        displayOrderDetails(order, controller.getDetails(orderId));
        System.out.println("1. Phê duyệt");
        System.out.println("2. Hủy");
        System.out.println("3. Hoàn tất giao hàng");
        System.out.println("0. Không thay đổi");
        int action = input.readInt("Chọn thao tác", 0);
        if (action == 0) {
            return;
        }
        try {
            boolean success = switch (action) {
                case 1 -> controller.approve(orderId);
                case 2 -> confirmCancel(orderId) && controller.cancel(orderId);
                case 3 -> controller.complete(orderId);
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ");
            };
            System.out.println(success ? "Cập nhật trạng thái thành công." : "Không có thay đổi.");
        } catch (Exception exception) {
            System.out.println("Không thể cập nhật đơn: " + exception.getMessage());
        }
    }

    private boolean confirmCancel(String orderId) {
        return "HUY".equalsIgnoreCase(
                input.readLine("Nhập HUY để xác nhận hủy đơn " + orderId + ": "));
    }

    private void displayOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("Chưa có đơn hàng.");
            return;
        }
        System.out.printf("%-12s %-12s %-20s %15s %-12s %-12s%n",
                "Mã đơn", "Khách", "Ngày", "Tổng tiền", "Trạng thái", "Sự kiện");
        System.out.println("-----------------------------------------------------------------------------------------");
        orders.stream().limit(DISPLAY_LIMIT).forEach(order ->
                System.out.printf("%-12s %-12s %-20s %,15d %-12s %-12s%n",
                        order.getOrderId(), order.getCustomerId(), order.getOrderDate(),
                        order.getTotalAmount(), order.getStatus(), order.getEventId()));
        if (orders.size() > DISPLAY_LIMIT) {
            System.out.println("... còn " + (orders.size() - DISPLAY_LIMIT) + " đơn.");
        }
    }

    private void displayOrderDetails(Order order, List<OrderDetail> details) {
        System.out.println("\nĐơn " + order.getOrderId() + " - " + order.getStatus());
        System.out.println("Khách hàng: " + order.getCustomerName());
        System.out.printf("Tổng thanh toán: %,d VND%n", order.getTotalAmount());
        System.out.printf("%-12s %-30s %8s %12s %14s%n",
                "Mã SP", "Tên", "SL", "Đơn giá", "Thành tiền");
        for (OrderDetail detail : details) {
            System.out.printf("%-12s %-30s %8d %,12d %,14d%n",
                    detail.getProductId(), trim(controller.getProductName(detail.getProductId()), 30),
                    detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal());
        }
    }

    private static String trim(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }
}
