package view;

import java.util.Map;

public class ReportView {
    public void displayDataGenerationResult(Map<String, Integer> result) {
        System.out.println("Đã tạo dữ liệu:");
        result.forEach((file, count) -> System.out.printf("- %s: %d dòng%n", file, count));
    }
}
