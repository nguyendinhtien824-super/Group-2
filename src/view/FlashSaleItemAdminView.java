package view;

import controller.FlashSaleController;
import controller.ProductController;
import model.FlashItem;
import model.Product;

import java.util.List;

/** Flash Sale item CRUD UI using controller APIs only. */
public class FlashSaleItemAdminView {
    private final FlashSaleController flashSaleController;
    private final ProductController productController;
    private final FlashSaleView tableView;
    private final ConsoleInput input;

    public FlashSaleItemAdminView(FlashSaleController flashSaleController,
                                  ProductController productController,
                                  FlashSaleView tableView,
                                  ConsoleInput input) {
        this.flashSaleController = flashSaleController;
        this.productController = productController;
        this.tableView = tableView;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> listByEvent();
                    case 2 -> createItem();
                    case 3 -> updateItem();
                    case 4 -> deleteItem();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác Flash Sale item.");
            } catch (Exception exception) {
                System.out.println("Không thể xử lý Flash Sale item: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- QUẢN LÝ FLASH SALE ITEM ---");
        System.out.println("1. Liệt kê theo sự kiện");
        System.out.println("2. Thêm sản phẩm vào sự kiện");
        System.out.println("3. Cập nhật giá/kho giới hạn");
        System.out.println("4. Xóa item");
        System.out.println("0. Quay lại");
    }

    private void listByEvent() {
        String eventId = input.readStringRequired("Mã sự kiện");
        tableView.displayItems(flashSaleController.getItemsByEvent(eventId));
    }

    private void createItem() throws Exception {
        String itemId = input.readStringRequired("Mã Flash Sale item");
        String eventId = input.readStringRequired("Mã sự kiện");
        flashSaleController.findEvent(eventId);
        String productId = input.readStringRequired("Mã sản phẩm");
        Product product = productController.findProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        int salePrice = input.readIntMin("Giá Flash Sale", 0, "Giá không được âm");
        int limitedStock = input.readIntMin("Số lượng giới hạn", 0, "Kho không được âm");
        FlashItem item = new FlashItem(itemId, productId, eventId, product.getName(),
                product.getPrice(), salePrice, limitedStock);
        flashSaleController.addFlashSaleItem(item);
        System.out.println("Đã thêm Flash Sale item.");
    }

    private void updateItem() throws Exception {
        String itemId = input.readStringRequired("Mã item cần sửa");
        FlashItem item = flashSaleController.findFlashSaleItem(itemId);
        if (item == null) {
            System.out.println("Không tìm thấy Flash Sale item.");
            return;
        }
        int expectedVersion = item.getVersion();
        int salePrice = readIntOrDefault("Giá sale", item.getSalePrice(), 0);
        int limitedStock = readIntOrDefault("Kho giới hạn", item.getInitialStock(), 0);
        item.setSalePrice(salePrice);
        item.setInitialStock(limitedStock);
        if (flashSaleController.updateFlashSaleItem(item, expectedVersion)) {
            System.out.println("Đã cập nhật item. Version mới: " + item.getVersion());
        } else {
            System.out.println("Version đã thay đổi ở phiên khác. Hãy tải lại rồi thử lại.");
        }
    }

    private void deleteItem() {
        String itemId = input.readStringRequired("Mã item cần xóa");
        if (!"XOA".equalsIgnoreCase(input.readLine("Nhập XOA để xác nhận: "))) {
            System.out.println("Đã hủy xóa.");
            return;
        }
        System.out.println(flashSaleController.deleteFlashSaleItem(itemId)
                ? "Đã xóa item." : "Không tìm thấy item.");
    }

    private int readIntOrDefault(String label, int current, int minimum) {
        String raw = input.readLine(label + " [" + current + "]: ");
        if (raw.isBlank()) {
            return current;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value < minimum) {
                throw new IllegalArgumentException(label + " phải từ " + minimum + " trở lên");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " phải là số nguyên", exception);
        }
    }
}

// Member 3
