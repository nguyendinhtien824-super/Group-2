package security;

import java.util.Optional;

/** Validation rules for newly created customer passwords. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 256;

    private PasswordPolicy() {
    }

    public static Optional<String> validationError(String password) {
        if (password == null || password.isBlank()) {
            return Optional.of("Mật khẩu là thông tin bắt buộc.");
        }

        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            return Optional.of("Mật khẩu phải từ " + MIN_LENGTH + " ký tự trở lên.");
        }
        if (length > MAX_LENGTH) {
            return Optional.of("Mật khẩu không được vượt quá " + MAX_LENGTH + " ký tự.");
        }
        return Optional.empty();
    }
}
