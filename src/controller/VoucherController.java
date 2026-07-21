package controller;

import model.Voucher;
import service.VoucherService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Thin presentation adapter for voucher administration. */
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = Objects.requireNonNull(voucherService, "voucherService");
    }

    public Result<List<Voucher>> list() {
        return execute(voucherService::list, "Đã tải danh sách voucher.");
    }

    public Result<List<Voucher>> listAvailable() {
        return execute(voucherService::listAvailable, "Đã tải voucher còn lượt sử dụng.");
    }

    public Result<List<Voucher>> search(String keyword) {
        return execute(() -> voucherService.search(keyword), "Tìm kiếm hoàn tất.");
    }

    public Result<Voucher> create(String code, String type, int value, int maxDiscount,
                                  int minOrderAmount, int remainingUses) {
        return execute(() -> voucherService.create(code, type, value, maxDiscount,
                minOrderAmount, remainingUses), "Tạo voucher thành công.");
    }

    public Result<Voucher> update(String idOrCode, String code, String type, Integer value,
                                  Integer maxDiscount, Integer minOrderAmount,
                                  Integer remainingUses) {
        return execute(() -> voucherService.update(idOrCode, code, type, value,
                maxDiscount, minOrderAmount, remainingUses), "Cập nhật voucher thành công.");
    }

    public Result<Voucher> delete(String idOrCode) {
        return execute(() -> voucherService.delete(idOrCode), "Xóa voucher thành công.");
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
