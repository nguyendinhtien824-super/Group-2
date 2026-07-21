package view;

import controller.CustomerController;
import controller.DataController;
import exception.EndOfInputException;
import exception.OperationCancelledException;
import model.Customer;
import security.AdminCredentials;
import security.SecurityEnvironment;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/** Top-level console router. Business logic stays in controllers and services. */
public class MainView {
    private static final int MAX_ADMIN_FAILURES = 3;
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final CustomerController customerController;
    private final DataController dataController;
    private final CustomerAccountView accountView;
    private final FlashSaleShoppingView shoppingView;
    private final OrderTrackingView trackingView;
    private final CustomerVoucherView voucherView;
    private final AdminView adminView;
    private final ResearcherView researcherView;
    private final ReportView reportView;
    private final Optional<AdminCredentials> adminCredentials;
    private final ConsoleInput input;

    private Customer loggedInCustomer;
    private int failedAdminAttempts;

    public MainView(CustomerController customerController,
                    DataController dataController,
                    CustomerAccountView accountView,
                    FlashSaleShoppingView shoppingView,
                    OrderTrackingView trackingView,
                    CustomerVoucherView voucherView,
                    AdminView adminView,
                    ResearcherView researcherView,
                    ReportView reportView,
                    Optional<AdminCredentials> adminCredentials,
                    ConsoleInput input) {
        this.customerController = customerController;
        this.dataController = dataController;
        this.accountView = accountView;
        this.shoppingView = shoppingView;
        this.trackingView = trackingView;
        this.voucherView = voucherView;
        this.adminView = adminView;
        this.researcherView = researcherView;
        this.reportView = reportView;
        this.adminCredentials = adminCredentials;
        this.input = input;
    }

    public void display() {
        boolean running = true;
        while (running) {
            refreshSession();
            printMenu();
            try {
                int choice = input.readInt("Chọn chức năng", 0);
                running = loggedInCustomer == null
                        ? handleGuestChoice(choice)
                        : handleCustomerChoice(choice);
            } catch (EndOfInputException exception) {
                running = false;
            } catch (OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác và quay về menu chính.");
            }
        }
        System.out.println("Đã thoát chương trình.");
    }

    private boolean handleGuestChoice(int choice) {
        switch (choice) {
            case 1 -> generateData();
            case 2 -> shoppingView.browse();
            case 3 -> accountView.register();
            case 4 -> loggedInCustomer = accountView.login().orElse(null);
            case 5 -> openAdmin();
            case 6 -> researcherView.display();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Lựa chọn không hợp lệ.");
        }
        return true;
    }

    private boolean handleCustomerChoice(int choice) {
        switch (choice) {
            case 1 -> shoppingView.browse();
            case 2 -> {
                shoppingView.book(loggedInCustomer);
                refreshSession();
            }
            case 3 -> trackingView.display(loggedInCustomer.getCustomerId());
            case 4 -> loggedInCustomer = accountView.displayAccountMenu(loggedInCustomer);
            case 5 -> voucherView.display();
            case 6 -> logout();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Lựa chọn không hợp lệ.");
        }
        return true;
    }

    private void printMenu() {
        System.out.println("\n===== LAB211 FLASH SALE CONSOLE =====");
        if (loggedInCustomer == null) {
            System.out.println("Trạng thái: Chưa đăng nhập");
            System.out.println("1. Tạo lại bộ dữ liệu CSV theo đề bài");
            System.out.println("2. Xem sự kiện và sản phẩm Flash Sale");
            System.out.println("3. Đăng ký khách hàng");
            System.out.println("4. Đăng nhập khách hàng");
            System.out.println("5. Đăng nhập quản trị viên");
            System.out.println("6. Khu vực nghiên cứu mô phỏng đồng thời");
        } else {
            System.out.printf("Khách hàng: %s (%s) | Hạng: %s | Ví: %,.0f VND%n",
                    loggedInCustomer.getName(), loggedInCustomer.getCustomerId(),
                    loggedInCustomer.getTier(), loggedInCustomer.getWalletBalance());
            System.out.println("1. Xem sự kiện và sản phẩm Flash Sale");
            System.out.println("2. Đặt hàng Flash Sale");
            System.out.println("3. Theo dõi đơn hàng");
            System.out.println("4. Hồ sơ, mật khẩu và ví");
            System.out.println("5. Voucher khả dụng");
            System.out.println("6. Đăng xuất");
        }
        System.out.println("0. Thoát");
    }

    private void generateData() {
        System.out.println("Cảnh báo: thao tác này thay thế bộ CSV nghiệp vụ hiện tại.");
        String confirmation = input.readLine("Nhập TAO để xác nhận, giá trị khác để quay lại: ");
        if (!"TAO".equalsIgnoreCase(confirmation)) {
            System.out.println("Đã giữ nguyên dữ liệu hiện tại.");
            return;
        }
        try {
            Map<String, Integer> result = dataController.generateData();
            reportView.displayDataGenerationResult(result);
        } catch (IOException exception) {
            System.out.println("Không thể tạo dữ liệu CSV. Hãy kiểm tra quyền ghi thư mục data.");
        }
    }

    private void openAdmin() {
        if (adminCredentials.isEmpty()) {
            System.out.printf("Admin đang bị vô hiệu hóa. Hãy cấu hình %s và %s.%n",
                    SecurityEnvironment.ADMIN_USERNAME,
                    SecurityEnvironment.ADMIN_PASSWORD_HASH);
            return;
        }
        if (failedAdminAttempts >= MAX_ADMIN_FAILURES) {
            System.out.println("Đăng nhập Admin đã bị khóa trong phiên này do nhập sai quá nhiều lần.");
            return;
        }

        String username = input.readStringRequired("Tên đăng nhập Admin");
        String password = input.readStringRequired("Mật khẩu Admin");
        if (adminCredentials.get().authenticate(username, password)) {
            failedAdminAttempts = 0;
            System.out.println("Đăng nhập Admin thành công.");
            adminView.display();
            return;
        }
        failedAdminAttempts++;
        System.out.printf("Thông tin đăng nhập không đúng. Còn %d lần thử trong phiên.%n",
                Math.max(0, MAX_ADMIN_FAILURES - failedAdminAttempts));
    }

    private void refreshSession() {
        if (loggedInCustomer == null) {
            return;
        }
        Optional<Customer> current = customerController.findCustomer(loggedInCustomer.getCustomerId());
        if (current.isEmpty() || !ACTIVE_STATUS.equalsIgnoreCase(current.get().getStatus())) {
            System.out.println("Tài khoản không còn hoạt động. Hệ thống đã đăng xuất phiên hiện tại.");
            loggedInCustomer = null;
            return;
        }
        loggedInCustomer = current.get();
    }

    private void logout() {
        loggedInCustomer = null;
        System.out.println("Đăng xuất thành công.");
    }
}

// Member 3
