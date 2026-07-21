package view;

import controller.OrderTrackingController;

import java.util.Scanner;

/** Router for customer order history, detail and transaction screens. */
public class OrderTrackingView {
    private final OrderHistoryView historyView;
    private final OrderDetailView detailView;
    private final ConsoleInput input;

    public OrderTrackingView(OrderTrackingController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public OrderTrackingView(OrderTrackingController controller, ConsoleInput input) {
        this.historyView = new OrderHistoryView(controller);
        this.detailView = new OrderDetailView(controller, input);
        this.input = input;
    }

    public void display(String customerId) {
        boolean back = false;
        while (!back) {
            printMenu();
            switch (input.readInt("Chọn chức năng", 0)) {
                case 1 -> historyView.displayOrders(customerId);
                case 2 -> detailView.display(customerId);
                case 3 -> historyView.displayTransactions(customerId);
                case 0 -> back = true;
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== THEO DÕI ĐƠN HÀNG CỦA TÔI =====");
        System.out.println("1. Danh sách đơn hàng");
        System.out.println("2. Xem chi tiết đơn hàng");
        System.out.println("3. Lịch sử giao dịch");
        System.out.println("0. Quay lại");
    }
}
