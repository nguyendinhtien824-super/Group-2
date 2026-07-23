package view;

import controller.CustomerController;
import model.Customer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Console-only input/output for customer authentication and profile actions. */
public class CustomerAccountView {
    private final CustomerController controller;
    private final ConsoleInput input;

    public CustomerAccountView(CustomerController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void register() {
        System.out.println("\n--- ĐĂNG KÝ KHÁCH HÀNG ---");
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("name", input.readStringRequired("Họ và tên"));
            payload.put("email", input.readStringPattern("Email",
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", "Email không đúng định dạng"));
            payload.put("phone", input.readStringPattern("Số điện thoại",
                    "^\\d{9,11}$", "Số điện thoại phải có 9–11 chữ số"));
            payload.put("address", input.readStringRequired("Địa chỉ"));
            payload.put("avatarUrl", input.readLine("URL ảnh đại diện (có thể bỏ trống): "));
            payload.put("password", input.readStringRequired("Mật khẩu (tối thiểu 6 ký tự)"));

            Map<String, Object> result = controller.register(payload);
            System.out.println(result.get("message"));
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy đăng ký.");
        }
    }

    public Optional<Customer> login() {
        System.out.println("\n--- ĐĂNG NHẬP KHÁCH HÀNG ---");
        try {
            String email = input.readStringRequired("Email");
            String password = input.readStringRequired("Mật khẩu");
            Optional<Customer> customer = controller.login(
                    Map.of("email", email, "password", password));
            System.out.println(customer.isPresent()
                    ? "Đăng nhập thành công."
                    : "Đăng nhập thất bại. Kiểm tra email, mật khẩu hoặc trạng thái tài khoản.");
            return customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy đăng nhập.");
            return Optional.empty();
        }
    }

    public Customer displayAccountMenu(Customer customer) {
        Customer current = customer;
        boolean back = false;
        while (!back) {
            printAccountMenu();
            int choice = input.readInt("Chọn chức năng", 0);
            switch (choice) {
                case 1 -> displayProfile(current);
                case 2 -> current = updateProfile(current);
                case 3 -> changePassword(current);
                case 4 -> current = topUpWallet(current);
                case 0 -> back = true;
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
        return current;
    }

    private void printAccountMenu() {
        System.out.println("\n--- TÀI KHOẢN CỦA TÔI ---");
        System.out.println("1. Xem thông tin");
        System.out.println("2. Cập nhật hồ sơ");
        System.out.println("3. Đổi mật khẩu");
        System.out.println("4. Nạp ví");
        System.out.println("0. Quay lại");
    }

    private void displayProfile(Customer customer) {
        System.out.println("Mã khách hàng: " + customer.getCustomerId());
        System.out.println("Họ tên: " + customer.getName());
        System.out.println("Email: " + customer.getEmail());
        System.out.println("Số điện thoại: " + customer.getPhone());
        System.out.println("Địa chỉ: " + customer.getAddress());
        System.out.println("Hạng: " + customer.getTier());
        System.out.println("Trạng thái: " + customer.getStatus());
        System.out.printf("Số dư ví: %,.0f VND%n", customer.getWalletBalance());
    }

    private Customer updateProfile(Customer customer) {
        try {
            String name = input.readLine("Họ tên mới (bỏ trống để giữ nguyên): ");
            String phone = input.readLine("Số điện thoại mới (bỏ trống để giữ nguyên): ");
            String address = input.readLine("Địa chỉ mới (bỏ trống để giữ nguyên): ");
            Map<String, Object> result = controller.updateProfile(customer.getCustomerId(),
                    Map.of("name", name, "phone", phone, "address", address));
            System.out.println(result.get("message"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? (Customer) result.get("customer") : customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy cập nhật hồ sơ.");
            return customer;
        }
    }

    private void changePassword(Customer customer) {
        try {
            String oldPassword = input.readStringRequired("Mật khẩu hiện tại");
            String newPassword = input.readStringRequired("Mật khẩu mới");
            String confirmation = input.readStringRequired("Nhập lại mật khẩu mới");
            if (!newPassword.equals(confirmation)) {
                System.out.println("Mật khẩu xác nhận không khớp.");
                return;
            }
            Map<String, Object> result = controller.changePassword(
                    customer.getCustomerId(), oldPassword, newPassword);
            System.out.println(result.get("message"));
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy đổi mật khẩu.");
        }
    }

    private Customer topUpWallet(Customer customer) {
        try {
            double amount = input.readDouble("Số tiền cần nạp", 100_000);
            Map<String, Object> result = controller.topUpWallet(customer.getCustomerId(), amount);
            System.out.println(result.get("message"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? (Customer) result.get("customer") : customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy nạp ví.");
            return customer;
        }
    }
}

// Member 3
