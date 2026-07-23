package view;

/** Top-level admin router. It owns no repository and no business rule. */
public class AdminView {
    private final ProductAdminView productView;
    private final FlashSaleEventAdminView eventView;
    private final FlashSaleItemAdminView itemView;
    private final AdminCustomerView customerView;
    private final AdminVoucherView voucherView;
    private final AdminOrderView orderView;
    private final AdminReportView reportView;
    private final ConsoleInput input;

    public AdminView(ProductAdminView productView,
                     FlashSaleEventAdminView eventView,
                     FlashSaleItemAdminView itemView,
                     AdminCustomerView customerView,
                     AdminVoucherView voucherView,
                     AdminOrderView orderView,
                     AdminReportView reportView,
                     ConsoleInput input) {
        this.productView = productView;
        this.eventView = eventView;
        this.itemView = itemView;
        this.customerView = customerView;
        this.voucherView = voucherView;
        this.orderView = orderView;
        this.reportView = reportView;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> productView.display();
                    case 2 -> eventView.display();
                    case 3 -> itemView.display();
                    case 4 -> customerView.display();
                    case 5 -> voucherView.display();
                    case 6 -> orderView.display();
                    case 7 -> reportView.display();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác quản trị.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== MENU QUẢN TRỊ VIÊN =====");
        System.out.println("1. Product CRUD + tìm theo danh mục/khoảng giá");
        System.out.println("2. Flash Sale Event CRUD + vòng đời");
        System.out.println("3. Flash Sale Item CRUD");
        System.out.println("4. Khách hàng CRUD + khóa/mở khóa");
        System.out.println("5. Voucher CRUD");
        System.out.println("6. Duyệt và quản lý đơn hàng");
        System.out.println("7. Báo cáo doanh thu/hạng/voucher/giao dịch");
        System.out.println("0. Quay lại");
    }
}

// Member 3
