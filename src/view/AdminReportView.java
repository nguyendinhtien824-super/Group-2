package view;

import controller.AdminReportController;
import model.enums.CustTier;
import model.enums.OrderStatus;
import service.AdminReportService;

import java.util.Objects;

/** Read-only console rendering for administrator analytics. */
public class AdminReportView {
    private final AdminReportController controller;

    public AdminReportView(AdminReportController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void display() {
        AdminReportController.Result result = controller.getReport();
        if (!result.success() || result.report() == null) {
            System.out.println("Lỗi: " + result.message());
            return;
        }
        AdminReportService.Report report = result.report();
        System.out.println("\n===== BÁO CÁO QUẢN TRỊ =====");
        System.out.printf("Doanh thu đơn SUCCESS: %,d VND%n", report.totalRevenue());
        System.out.printf("Đơn thành công: %,d | Đơn trạng thái khác: %,d%n",
                report.successfulOrders(), report.otherOrders());

        System.out.println("\n--- ĐƠN HÀNG THEO TRẠNG THÁI ---");
        for (OrderStatus status : OrderStatus.values()) {
            System.out.printf("%-18s: %,d%n", status,
                    report.ordersByStatus().getOrDefault(status, 0L));
        }

        System.out.println("\n--- KHÁCH HÀNG THEO HẠNG ---");
        for (CustTier tier : CustTier.values()) {
            System.out.printf("%-18s: %,d%n", tier,
                    report.customersByTier().getOrDefault(tier, 0L));
        }

        System.out.println("\n--- THỐNG KÊ VOUCHER ---");
        if (report.vouchers().isEmpty()) {
            System.out.println("Chưa có voucher.");
            return;
        }
        System.out.printf("%-14s | %-10s | %10s | %12s | %10s | %12s%n",
                "Mã", "Loại", "Giá trị", "Giảm tối đa", "Còn lại", "Đơn SUCCESS");
        System.out.println("--------------------------------------------------------------------------------");
        for (AdminReportService.VoucherMetric voucher : report.vouchers()) {
            System.out.printf("%-14s | %-10s | %,10d | %,12d | %,10d | %,12d%n",
                    voucher.code(), voucher.type(), voucher.value(), voucher.maxDiscount(),
                    voucher.remainingUses(), voucher.successfulUses());
        }
    }
}
