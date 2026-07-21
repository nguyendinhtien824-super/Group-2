package security;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

/** Interactive helper that prints an Argon2id hash without accepting a password argument. */
public final class AdminPasswordTool {
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private AdminPasswordTool() {
    }

    public static void run() {
        char[] password = readPassword();
        try {
            if (password.length < MIN_ADMIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException(
                        "Mật khẩu Admin phải có ít nhất 12 ký tự.");
            }
            System.out.println(PasswordSecurity.hash(new String(password)));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readPassword() {
        Console console = System.console();
        if (console != null) {
            char[] value = console.readPassword("Nhập mật khẩu Admin cần băm: ");
            if (value == null) {
                throw new IllegalStateException("Đã hủy tạo mật khẩu Admin.");
            }
            return value;
        }

        System.err.println("Cảnh báo: terminal hiện tại không hỗ trợ ẩn ký tự nhập.");
        System.out.print("Nhập mật khẩu Admin cần băm: ");
        Scanner scanner = new Scanner(System.in);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Không đọc được mật khẩu Admin.");
        }
        return scanner.nextLine().toCharArray();
    }
}
