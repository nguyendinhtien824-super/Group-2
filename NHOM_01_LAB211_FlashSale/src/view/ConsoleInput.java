package view;

import model.enums.LockType;

import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
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
                System.out.println("Gia tri khong hop le. Vui long nhap so nguyen.");
            }
        }
    }

    public LockType readLockType(String prompt, LockType defaultValue) {
        while (true) {
            System.out.println("Cac co che:");
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
                System.out.println("Co che khong hop le.");
            }
        }
    }
}
