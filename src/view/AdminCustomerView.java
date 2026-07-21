package view;

import controller.AdminCustomerController;
import exception.OperationCancelledException;
import model.Customer;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Console-only presentation for administrator customer management. */
public class AdminCustomerView {
    private final AdminCustomerController controller;
    private final ConsoleInput input;

    public AdminCustomerView(AdminCustomerController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public AdminCustomerView(AdminCustomerController controller, ConsoleInput input) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Nhập lựa chọn của bạn", 0)) {
                    case 1 -> showCustomers(controller.list());
                    case 2 -> search();
                    case 3 -> create();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> changeStatus("BANNED");
                    case 7 -> changeStatus("ACTIVE");
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác hiện tại.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== QUẢN LÝ TÀI KHOẢN KHÁCH HÀNG =====");
        System.out.println("1. Xem danh sách khách hàng");
        System.out.println("2. Tìm kiếm khách hàng");
        System.out.println("3. Thêm khách hàng");
        System.out.println("4. Cập nhật khách hàng");
        System.out.println("5. Xóa khách hàng");
        System.out.println("6. Khóa tài khoản");
        System.out.println("7. Mở khóa tài khoản");
        System.out.println("0. Quay lại");
    }

    private void search() {
        String keyword = input.readStringRequired("Nhập ID, tên, email hoặc số điện thoại");
        showCustomers(controller.search(keyword));
    }

    private void create() {
        String name = input.readStringRequired("Họ tên");
        String email = input.readStringRequired("Email");
        String phone = input.readStringRequired("Số điện thoại");
        String address = input.readStringRequired("Địa chỉ");
        String avatarUrl = input.readLine("Avatar URL (có thể bỏ trống): ");
        String tier = input.readLine("Hạng STANDARD/SILVER/GOLD/DIAMOND [STANDARD]: ");
        String password = input.readStringRequired("Mật khẩu");
        printAction(controller.create(name, email, phone, address, avatarUrl, tier, password));
    }

    private void update() {
        String customerId = input.readStringRequired("ID khách hàng cần cập nhật");
        System.out.println("Bỏ trống trường không muốn thay đổi.");
        String name = input.readLine("Họ tên mới: ");
        String email = input.readLine("Email mới: ");
        String phone = input.readLine("Số điện thoại mới: ");
        String address = input.readLine("Địa chỉ mới: ");
        String avatarUrl = input.readLine("Avatar URL mới: ");
        String tier = input.readLine("Hạng mới: ");
        String password = input.readLine("Mật khẩu mới: ");
        printAction(controller.update(customerId, name, email, phone, address,
                avatarUrl, tier, password));
    }

    private void delete() {
        String customerId = input.readStringRequired("ID khách hàng cần xóa");
        String confirmation = input.readLine(
                "Xóa tài khoản và hủy các đơn chưa hoàn thành? Nhập Y để xác nhận: ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            System.out.println("Đã hủy thao tác xóa.");
            return;
        }
        printAction(controller.delete(customerId));
    }

    private void changeStatus(String status) {
        String customerId = input.readStringRequired("ID khách hàng");
        printAction(controller.setStatus(customerId, status));
    }

    private void showCustomers(AdminCustomerController.Result<List<Customer>> result) {
        if (!result.success()) {
            System.out.println("Lỗi: " + result.message());
            return;
        }
        List<Customer> customers = result.data();
        if (customers == null || customers.isEmpty()) {
            System.out.println("Không có khách hàng phù hợp.");
            return;
        }
        System.out.printf("%-10s | %-20s | %-28s | %-12s | %-9s | %-8s%n",
                "ID", "Họ tên", "Email", "Điện thoại", "Hạng", "Trạng thái");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (Customer customer : customers) {
            System.out.printf("%-10s | %-20s | %-28s | %-12s | %-9s | %-8s%n",
                    text(customer.getCustomerId(), 10), text(customer.getName(), 20),
                    text(customer.getEmail(), 28), text(customer.getPhone(), 12),
                    customer.getTier(), text(customer.getStatus(), 8));
        }
    }

    private void printAction(AdminCustomerController.Result<?> result) {
        System.out.println((result.success() ? "Thành công: " : "Lỗi: ") + result.message());
    }

    private static String text(String value, int width) {
        String safe = value == null ? "" : value;
        return safe.length() <= width ? safe : safe.substring(0, width - 3) + "...";
    }
}
