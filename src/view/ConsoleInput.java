package view;

import model.enums.LockType;
import exception.EndOfInputException;
import exception.OperationCancelledException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ConsoleInput {
    private final Scanner scanner;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();
        }
        String input = scanner.nextLine().trim();
        if ("cancel".equalsIgnoreCase(input)) {
            throw new OperationCancelledException();
        }
        return input;
    }

    public String readStringRequired(String prompt) {
        while (true) {
            String value = readLine(prompt + ": ");
            if (value.isEmpty()) {
                System.out.println("Lỗi: Trường này là bắt buộc, không được để trống.");
                continue;
            }
            return value;
        }
    }

    public String readStringPattern(String prompt, String regex, String errorMsg) {
        Pattern pattern = Pattern.compile(regex);
        while (true) {
            String value = readLine(prompt + ": ");
            if (value.isEmpty()) {
                System.out.println("Lỗi: Trường này là bắt buộc, không được để trống.");
                continue;
            }
            if (!pattern.matcher(value).matches()) {
                System.out.println("Lỗi: " + errorMsg);
                continue;
            }
            return value;
        }
    }

    public String readDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (yyyy-MM-dd HH:mm:ss): ");
            if (value.isEmpty()) {
                System.out.println("Lỗi: Thời gian không được để trống.");
                continue;
            }
            try {
                LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                return value;
            } catch (DateTimeParseException e) {
                System.out.println("Lỗi: Thời gian không đúng định dạng yyyy-MM-dd HH:mm:ss (Ví dụ: 2026-06-29 12:00:00).");
            }
        }
    }

    public int readInt(String prompt, int defaultValue) {
        while (true) {
            String value = readLine(prompt + " [" + defaultValue + "]: ");
            if (value.isEmpty()) {
                return defaultValue;
            }

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Giá trị không hợp lệ. Vui lòng nhập số nguyên.");
            }
        }
    }

    public int readIntRequired(String prompt) {
        while (true) {
            String value = readLine(prompt + ": ");
            if (value.isEmpty()) {
                System.out.println("Giá trị bắt buộc, vui lòng không để trống.");
                continue;
            }

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Giá trị không hợp lệ. Vui lòng nhập số nguyên.");
            }
        }
    }

    public int readIntMin(String prompt, int min, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value < min) {
                System.out.println("Lỗi: " + errorMsg);
                continue;
            }
            return value;
        }
    }

    public int readIntMinMax(String prompt, int min, int max, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value < min || value > max) {
                System.out.println("Lỗi: " + errorMsg);
                continue;
            }
            return value;
        }
    }

    /**
     * Đọc số thực (double) với giá trị mặc định nếu người dùng bỏ trống.
     */
    public double readDouble(String prompt, double defaultValue) {
        while (true) {
            String value = readLine(prompt + " [" + defaultValue + "]: ");
            if (value.isEmpty()) {
                return defaultValue;
            }
            try {
                double parsed = Double.parseDouble(value.replace(",", ""));
                if (!Double.isFinite(parsed)) {
                    System.out.println("Giá trị phải là một số hữu hạn.");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
                System.out.println("Giá trị không hợp lệ. Vui lòng nhập số (ví dụ: 100000).");
            }
        }
    }

    public LockType readLockType(String prompt, LockType defaultValue) {
        while (true) {
            System.out.println("Các cơ chế khóa:");
            for (LockType type : LockType.values()) {
                System.out.println("- " + type.name());
            }

            String value = readLine(prompt + " [" + defaultValue.name() + "]: ");
            if (value.isEmpty()) {
                return defaultValue;
            }

            try {
                return LockType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Cơ chế khóa không hợp lệ.");
            }
        }
    }
}

// Member 3
