package view;

import controller.ProductController;
import model.Product;

import java.util.List;

/** Product CRUD and category/price search through ProductController. */
public class ProductAdminView {
    private static final int DISPLAY_LIMIT = 20;

    private final ProductController controller;
    private final ConsoleInput input;

    public ProductAdminView(ProductController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> displayProducts(controller.listProducts());
                    case 2 -> createProduct();
                    case 3 -> updateProduct();
                    case 4 -> deleteProduct();
                    case 5 -> searchProducts();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác sản phẩm.");
            } catch (IllegalArgumentException exception) {
                System.out.println("Không thể xử lý sản phẩm: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- QUẢN LÝ SẢN PHẨM ---");
        System.out.println("1. Danh sách sản phẩm");
        System.out.println("2. Thêm sản phẩm");
        System.out.println("3. Cập nhật sản phẩm");
        System.out.println("4. Xóa sản phẩm");
        System.out.println("5. Tìm theo danh mục và khoảng giá");
        System.out.println("0. Quay lại");
    }

    private void createProduct() {
        String id = input.readStringRequired("Mã sản phẩm");
        Product product = readProduct(id, null);
        controller.createProduct(product);
        System.out.println("Đã thêm sản phẩm " + id + ".");
    }

    private void updateProduct() {
        String id = input.readStringRequired("Mã sản phẩm cần sửa");
        Product current = controller.findProduct(id);
        if (current == null) {
            System.out.println("Không tìm thấy sản phẩm.");
            return;
        }
        int expectedVersion = current.getVersion();
        Product updated = readProduct(id, current);
        if (controller.updateProduct(updated, expectedVersion)) {
            System.out.println("Cập nhật thành công. Version mới: " + updated.getVersion());
        } else {
            System.out.println("Dữ liệu vừa bị thay đổi ở phiên khác. Hãy tải lại rồi thử lại.");
        }
    }

    private void deleteProduct() {
        String id = input.readStringRequired("Mã sản phẩm cần xóa");
        if (controller.findProduct(id) == null) {
            System.out.println("Không tìm thấy sản phẩm.");
            return;
        }
        String confirmation = input.readLine("Nhập XOA để xác nhận: ");
        if (!"XOA".equalsIgnoreCase(confirmation)) {
            System.out.println("Đã hủy xóa.");
            return;
        }
        System.out.println(controller.deleteProduct(id) ? "Đã xóa sản phẩm." : "Xóa thất bại.");
    }

    private void searchProducts() {
        String category = input.readLine("Danh mục (bỏ trống để lấy tất cả): ");
        int minimumPrice = input.readIntMin("Giá tối thiểu", 0, "Giá không được âm");
        int maximumPrice = input.readIntMin("Giá tối đa", minimumPrice,
                "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu");
        displayProducts(controller.searchProducts(category, minimumPrice, maximumPrice));
    }

    private Product readProduct(String id, Product current) {
        String name = readTextOrDefault("Tên", current == null ? null : current.getName());
        String brand = readTextOrDefault("Thương hiệu", current == null ? null : current.getBrand());
        String category = readTextOrDefault("Danh mục", current == null ? null : current.getCategory());
        int price = readIntOrDefault("Giá", current == null ? null : current.getPrice(), 1);
        int stock = readIntOrDefault("Tồn kho", current == null ? null : current.getStock(), 0);
        String description = readTextOrDefault("Mô tả",
                current == null ? "" : current.getDescription());
        int version = current == null ? 0 : current.getVersion();
        return new Product(id, name, brand, category, price, stock, description, version);
    }

    private String readTextOrDefault(String label, String current) {
        if (current == null) {
            return input.readStringRequired(label);
        }
        String value = input.readLine(label + " [" + current + "]: ");
        return value.isBlank() ? current : value;
    }

    private int readIntOrDefault(String label, Integer current, int minimum) {
        if (current == null) {
            return input.readIntMin(label, minimum, label + " không hợp lệ");
        }
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

    private void displayProducts(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("Không có sản phẩm phù hợp.");
            return;
        }
        System.out.printf("%-10s %-25s %-15s %-15s %12s %8s %8s%n",
                "Mã", "Tên", "Thương hiệu", "Danh mục", "Giá", "Kho", "Version");
        System.out.println("------------------------------------------------------------------------------------------------------");
        products.stream().limit(DISPLAY_LIMIT).forEach(product ->
                System.out.printf("%-10s %-25s %-15s %-15s %,12d %8d %8d%n",
                        product.getProductId(), trim(product.getName(), 25),
                        trim(product.getBrand(), 15), trim(product.getCategory(), 15),
                        product.getPrice(), product.getStock(), product.getVersion()));
        if (products.size() > DISPLAY_LIMIT) {
            System.out.println("... còn " + (products.size() - DISPLAY_LIMIT) + " sản phẩm.");
        }
    }

    private static String trim(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value == null ? "" : value;
        }
        return value.substring(0, limit - 3) + "...";
    }
}

// Member 3
