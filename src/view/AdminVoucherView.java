package view;

import controller.VoucherController;
import exception.OperationCancelledException;
import model.Voucher;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Console-only presentation for voucher administration. */
public class AdminVoucherView {
    private final VoucherController controller;
    private final ConsoleInput input;

    public AdminVoucherView(VoucherController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public AdminVoucherView(VoucherController controller, ConsoleInput input) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Nhập lựa chọn của bạn", 0)) {
                    case 1 -> showVouchers(controller.list());
                    case 2 -> search();
                    case 3 -> create();
                    case 4 -> update();
                    case 5 -> delete();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác hiện tại.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== QUẢN LÝ VOUCHER =====");
        System.out.println("1. Xem danh sách voucher");
        System.out.println("2. Tìm kiếm voucher");
        System.out.println("3. Thêm voucher");
        System.out.println("4. Cập nhật voucher");
        System.out.println("5. Xóa voucher");
        System.out.println("0. Quay lại");
    }

    private void search() {
        String keyword = input.readStringRequired("Nhập ID, mã hoặc loại voucher");
        showVouchers(controller.search(keyword));
    }

    private void create() {
        String code = input.readStringRequired("Mã voucher");
        String type = input.readStringRequired("Loại PERCENTAGE/FIXED");
        int value = input.readIntRequired("Giá trị giảm");
        int maxDiscount = input.readIntRequired("Mức giảm tối đa");
        int minOrder = input.readIntRequired("Giá trị đơn tối thiểu");
        int uses = input.readIntRequired("Số lượt còn lại");
        printAction(controller.create(code, type, value, maxDiscount, minOrder, uses));
    }

    private void update() {
        String key = input.readStringRequired("ID hoặc mã voucher cần cập nhật");
        System.out.println("Bỏ trống trường không muốn thay đổi.");
        String code = input.readLine("Mã mới: ");
        String type = input.readLine("Loại mới PERCENTAGE/FIXED: ");
        Integer value = readOptionalInt("Giá trị giảm mới: ");
        Integer maxDiscount = readOptionalInt("Mức giảm tối đa mới: ");
        Integer minOrder = readOptionalInt("Giá trị đơn tối thiểu mới: ");
        Integer uses = readOptionalInt("Số lượt còn lại mới: ");
        printAction(controller.update(key, code, type, value, maxDiscount, minOrder, uses));
    }

    private void delete() {
        String key = input.readStringRequired("ID hoặc mã voucher cần xóa");
        String confirmation = input.readLine("Nhập Y để xác nhận xóa: ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            System.out.println("Đã hủy thao tác xóa.");
            return;
        }
        printAction(controller.delete(key));
    }

    private Integer readOptionalInt(String prompt) {
        while (true) {
            String value = input.readLine(prompt);
            if (value.isBlank()) {
                return null;
            }
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException exception) {
                System.out.println("Vui lòng nhập số nguyên hoặc bỏ trống.");
            }
        }
    }

    private void showVouchers(VoucherController.Result<List<Voucher>> result) {
        if (!result.success()) {
            System.out.println("Lỗi: " + result.message());
            return;
        }
        List<Voucher> vouchers = result.data();
        if (vouchers == null || vouchers.isEmpty()) {
            System.out.println("Không có voucher phù hợp.");
            return;
        }
        System.out.printf("%-9s | %-15s | %-10s | %10s | %12s | %12s | %8s%n",
                "ID", "Mã", "Loại", "Giá trị", "Giảm tối đa", "Đơn tối thiểu", "Còn lại");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (Voucher voucher : vouchers) {
            System.out.printf("%-9s | %-15s | %-10s | %,10d | %,12d | %,12d | %,8d%n",
                    voucher.getVoucherId(), voucher.getCode(), voucher.getType(),
                    voucher.getValue(), voucher.getMaxDiscount(),
                    voucher.getMinOrderAmount(), voucher.getRemainingUses());
        }
    }

    private void printAction(VoucherController.Result<?> result) {
        System.out.println((result.success() ? "Thành công: " : "Lỗi: ") + result.message());
    }
}
