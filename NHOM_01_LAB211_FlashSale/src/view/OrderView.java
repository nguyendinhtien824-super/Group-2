package view;

public class OrderView {
    public void displayBookingResult(boolean success) {
        if (success) {
            System.out.println("Dat hang thanh cong.");
        } else {
            System.out.println("Dat hang that bai. Kiem tra ton kho, itemId hoac gioi han so luong.");
        }
    }

    public void displayBookingError(Exception e) {
        System.out.println("Dat hang that bai: " + e.getMessage());
    }
}
