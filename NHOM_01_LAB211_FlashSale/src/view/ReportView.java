package view;

import java.util.Map;

public class ReportView {
    public void displayDataGenerationResult(Map<String, Integer> result) {
        System.out.println("Da tao du lieu:");
        result.forEach((file, count) -> System.out.printf("- %s: %d dong%n", file, count));
    }
}
