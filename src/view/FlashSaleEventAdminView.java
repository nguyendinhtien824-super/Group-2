package view;

import controller.FlashSaleController;
import model.FlashSaleEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Event lifecycle and CRUD UI; all rules are enforced below the View layer. */
public class FlashSaleEventAdminView {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FlashSaleController controller;
    private final ConsoleInput input;

    public FlashSaleEventAdminView(FlashSaleController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Chọn chức năng", 0)) {
                    case 1 -> displayEvents(controller.listEvents());
                    case 2 -> createEvent();
                    case 3 -> updateEvent();
                    case 4 -> startEvent();
                    case 5 -> endEvent();
                    case 6 -> lockEvent();
                    case 7 -> unlockEvent();
                    case 8 -> deleteEvent();
                    case 0 -> back = true;
                    default -> System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Đã hủy thao tác sự kiện.");
            } catch (Exception exception) {
                System.out.println("Không thể xử lý sự kiện: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- QUẢN LÝ SỰ KIỆN FLASH SALE ---");
        System.out.println("1. Danh sách sự kiện");
        System.out.println("2. Tạo sự kiện 1–2 giờ");
        System.out.println("3. Cập nhật sự kiện chưa chạy");
        System.out.println("4. Bắt đầu sự kiện");
        System.out.println("5. Kết thúc sự kiện");
        System.out.println("6. Tạm khóa sự kiện");
        System.out.println("7. Mở khóa sự kiện");
        System.out.println("8. Xóa sự kiện");
        System.out.println("0. Quay lại");
    }

    private void createEvent() throws Exception {
        String id = input.readStringRequired("Mã sự kiện");
        String name = input.readStringRequired("Tên sự kiện");
        String start = input.readDateTime("Thời gian bắt đầu");
        String end = input.readDateTime("Thời gian kết thúc");
        controller.createEvent(new FlashSaleEvent(id, name, start, end, "UPCOMING"));
        System.out.println("Đã tạo sự kiện.");
    }

    private void updateEvent() throws Exception {
        String id = input.readStringRequired("Mã sự kiện cần sửa");
        FlashSaleEvent current = controller.findEvent(id);
        String name = readOrDefault("Tên", current.getName());
        String start = readDateOrDefault("Bắt đầu", current.getStartTime());
        String end = readDateOrDefault("Kết thúc", current.getEndTime());
        controller.updateEvent(new FlashSaleEvent(id, name, start, end, current.getStatus()));
        System.out.println("Đã cập nhật sự kiện.");
    }

    private void startEvent() throws Exception {
        controller.startEvent(input.readStringRequired("Mã sự kiện cần bắt đầu"));
        System.out.println("Sự kiện đã bắt đầu.");
    }

    private void endEvent() throws Exception {
        controller.endEvent(input.readStringRequired("Mã sự kiện cần kết thúc"));
        System.out.println("Sự kiện đã kết thúc.");
    }

    private void lockEvent() throws Exception {
        String id = input.readStringRequired("Mã sự kiện cần tạm khóa");
        LocalDateTime unlockAt = LocalDateTime.parse(
                input.readDateTime("Thời điểm tự mở khóa"), FORMATTER);
        controller.lockEvent(id, unlockAt);
        System.out.println("Sự kiện đã tạm khóa.");
    }

    private void unlockEvent() throws Exception {
        controller.unlockEvent(input.readStringRequired("Mã sự kiện cần mở khóa"));
        System.out.println("Sự kiện đã mở khóa.");
    }

    private void deleteEvent() throws Exception {
        String id = input.readStringRequired("Mã sự kiện cần xóa");
        if (!"XOA".equalsIgnoreCase(input.readLine("Nhập XOA để xác nhận: "))) {
            System.out.println("Đã hủy xóa.");
            return;
        }
        System.out.println(controller.deleteEvent(id) ? "Đã xóa sự kiện." : "Xóa thất bại.");
    }

    private String readOrDefault(String label, String current) {
        String value = input.readLine(label + " [" + current + "]: ");
        return value.isBlank() ? current : value;
    }

    private String readDateOrDefault(String label, String current) {
        String value = input.readLine(label + " [" + current + "] (yyyy-MM-dd HH:mm:ss): ");
        if (value.isBlank()) {
            return current;
        }
        LocalDateTime.parse(value, FORMATTER);
        return value;
    }

    private void displayEvents(List<FlashSaleEvent> events) {
        if (events.isEmpty()) {
            System.out.println("Chưa có sự kiện.");
            return;
        }
        System.out.printf("%-10s %-25s %-20s %-20s %-12s%n",
                "Mã", "Tên", "Bắt đầu", "Kết thúc", "Trạng thái");
        System.out.println("--------------------------------------------------------------------------------------------");
        for (FlashSaleEvent event : events) {
            System.out.printf("%-10s %-25s %-20s %-20s %-12s%n",
                    event.getEventId(), trim(event.getName(), 25), event.getStartTime(),
                    event.getEndTime(), event.getStatus());
        }
    }

    private static String trim(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }
}

// Member 3
