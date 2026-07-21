package view;

import controller.VoucherController;
import model.Voucher;

import java.util.List;

/** Read-only voucher list for signed-in customers. */
public class CustomerVoucherView {
    private final VoucherController controller;

    public CustomerVoucherView(VoucherController controller) {
        this.controller = controller;
    }

    public void display() {
        VoucherController.Result<List<Voucher>> result = controller.listAvailable();
        if (!result.success() || result.data() == null) {
            System.out.println("Không thể tải voucher: " + result.message());
            return;
        }
        if (result.data().isEmpty()) {
            System.out.println("Hiện không có voucher còn lượt sử dụng.");
            return;
        }

        System.out.println("\n--- VOUCHER KHẢ DỤNG ---");
        System.out.printf("%-15s | %-12s | %12s | %14s | %14s | %10s%n",
                "Mã", "Loại", "Giá trị", "Giảm tối đa", "Đơn tối thiểu", "Còn lượt");
        System.out.println("-".repeat(92));
        for (Voucher voucher : result.data()) {
            System.out.printf("%-15s | %-12s | %,12d | %,14d | %,14d | %,10d%n",
                    voucher.getCode(), voucher.getType(), voucher.getValue(),
                    voucher.getMaxDiscount(), voucher.getMinOrderAmount(),
                    voucher.getRemainingUses());
        }
    }
}
