package controller;

import model.Customer;
import service.CustomerAdminService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Thin presentation adapter for administrator customer operations. */
public class AdminCustomerController {
    private final CustomerAdminService customerAdminService;

    public AdminCustomerController(CustomerAdminService customerAdminService) {
        this.customerAdminService = Objects.requireNonNull(
                customerAdminService, "customerAdminService");
    }

    public Result<List<Customer>> list() {
        return execute(customerAdminService::list, "Đã tải danh sách khách hàng.");
    }

    public Result<List<Customer>> search(String keyword) {
        return execute(() -> customerAdminService.search(keyword), "Tìm kiếm hoàn tất.");
    }

    public Result<Customer> create(String name, String email, String phone, String address,
                                   String avatarUrl, String tier, String password) {
        return execute(
                () -> customerAdminService.create(
                        name, email, phone, address, avatarUrl, tier, password),
                "Tạo khách hàng thành công.");
    }

    public Result<Customer> update(String customerId, String name, String email, String phone,
                                   String address, String avatarUrl, String tier,
                                   String newPassword) {
        return execute(
                () -> customerAdminService.update(customerId, name, email, phone,
                        address, avatarUrl, tier, newPassword),
                "Cập nhật khách hàng thành công.");
    }

    public Result<Customer> setStatus(String customerId, String status) {
        return execute(() -> customerAdminService.setStatus(customerId, status),
                "Cập nhật trạng thái thành công.");
    }

    public Result<Customer> delete(String customerId) {
        return execute(() -> customerAdminService.delete(customerId),
                "Xóa khách hàng thành công.");
    }

    private static <T> Result<T> execute(Supplier<T> action, String successMessage) {
        try {
            return new Result<>(true, successMessage, action.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return new Result<>(false, exception.getMessage(), null);
        }
    }

    public record Result<T>(boolean success, String message, T data) {
    }
}

// Member 3
