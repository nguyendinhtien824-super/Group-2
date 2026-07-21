package view;

import controller.FlashSaleController;
import controller.OrderController;
import model.Customer;
import model.FlashItem;
import model.FlashSaleEvent;

import java.util.List;

/** Customer-facing Flash Sale browsing and ordering console. */
public class FlashSaleShoppingView {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 2;

    private final FlashSaleController flashSaleController;
    private final OrderController orderController;
    private final FlashSaleView itemTable;
    private final OrderView orderView;
    private final ConsoleInput input;

    public FlashSaleShoppingView(FlashSaleController flashSaleController,
                                 OrderController orderController,
                                 FlashSaleView itemTable,
                                 OrderView orderView,
                                 ConsoleInput input) {
        this.flashSaleController = flashSaleController;
        this.orderController = orderController;
        this.itemTable = itemTable;
        this.orderView = orderView;
        this.input = input;
    }

    public void browse() {
        List<FlashSaleEvent> events = flashSaleController.listCustomerVisibleEvents();
        if (events.isEmpty()) {
            System.out.println("Hiện không có sự kiện Flash Sale đang hoạt động.");
            return;
        }

        printEvents(events);
        FlashSaleEvent selected = selectEvent(events);
        if (selected == null) {
            System.out.println("Không tìm thấy sự kiện đã chọn.");
            return;
        }

        System.out.printf("%n--- SẢN PHẨM: %s (%s) ---%n",
                selected.getName(), selected.getEventId());
        itemTable.displayItems(flashSaleController.getItemsByEvent(selected.getEventId()));
    }

    public boolean book(Customer customer) {
        if (customer == null) {
            System.out.println("Bạn cần đăng nhập trước khi đặt hàng.");
            return false;
        }

        try {
            String itemId = input.readStringRequired("Mã Flash Sale item");
            FlashItem item = flashSaleController.findFlashSaleItem(itemId);
            if (item == null) {
                System.out.println("Mã Flash Sale item không tồn tại.");
                return false;
            }

            System.out.printf("%s | Giá sale: %,d VND | Còn lại: %d%n",
                    item.getProductName(), item.getSalePrice(), item.getRemainingStock());
            int quantity = input.readIntMinMax("Số lượng", MIN_QUANTITY, MAX_QUANTITY,
                    "Mỗi khách chỉ được mua từ 1 đến 2 sản phẩm cho mỗi event.");
            String voucherCode = input.readLine("Mã voucher (bỏ trống nếu không dùng): ");
            String normalizedVoucher = voucherCode.isBlank() ? null : voucherCode;

            boolean success = orderController.bookItem(
                    itemId, quantity, customer.getCustomerId(), normalizedVoucher);
            orderView.displayBookingResult(success);
            return success;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Đã hủy đặt hàng.");
            return false;
        } catch (Exception exception) {
            orderView.displayBookingError(exception);
            return false;
        }
    }

    private void printEvents(List<FlashSaleEvent> events) {
        System.out.println("\n--- SỰ KIỆN FLASH SALE ĐANG HIỂN THỊ ---");
        System.out.printf("%-4s | %-14s | %-28s | %-19s | %-19s | %-10s%n",
                "STT", "Mã", "Tên", "Bắt đầu", "Kết thúc", "Trạng thái");
        System.out.println("-".repeat(108));
        for (int index = 0; index < events.size(); index++) {
            FlashSaleEvent event = events.get(index);
            System.out.printf("%-4d | %-14s | %-28s | %-19s | %-19s | %-10s%n",
                    index + 1, event.getEventId(), trim(event.getName(), 28),
                    event.getStartTime(), event.getEndTime(), event.getStatus());
        }
    }

    private FlashSaleEvent selectEvent(List<FlashSaleEvent> events) {
        String selection = input.readStringRequired("Nhập STT hoặc mã sự kiện");
        try {
            int index = Integer.parseInt(selection);
            if (index >= 1 && index <= events.size()) {
                return events.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // The same input may be an event identifier.
        }
        return events.stream()
                .filter(event -> event.getEventId().equalsIgnoreCase(selection))
                .findFirst()
                .orElse(null);
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
