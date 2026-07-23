package view;

public class OrderView {
    public void displayBookingResult(boolean success) {
        if (success) {
            System.out.println("Đặt hàng thành công.");
        } else {
            System.out.println("Đặt hàng thất bại. Kiểm tra tồn kho, mã sản phẩm hoặc giới hạn số lượng.");
        }
    }

    public void displayBookingError(Exception e) {
        System.out.println("Đặt hàng thất bại: " + e.getMessage());
    }
}

// Member 3
