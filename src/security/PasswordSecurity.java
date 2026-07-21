package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Centralized Argon2id hashing and legacy-password verification. */
public final class PasswordSecurity {
    private static final String ARGON2_ID_PREFIX = "$argon2id$";
    private static final PasswordEncoder ENCODER = new Argon2PasswordEncoder(
            16,
            32,
            1,
            19 * 1024,
            2);

    private PasswordSecurity() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (!isEncodedHash(storedPassword)) {
            return constantTimeEquals(rawPassword, storedPassword);
        }
        try {
            return ENCODER.matches(rawPassword, storedPassword);
        } catch (IllegalArgumentException invalidHash) {
            return false;
        }
    }

    public static boolean isEncodedHash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(ARGON2_ID_PREFIX);
    }

    public static boolean needsRehash(String storedPassword) {
        if (!isEncodedHash(storedPassword)) {
            return true;
        }
        try {
            return ENCODER.upgradeEncoding(storedPassword);
        } catch (IllegalArgumentException invalidHash) {
            return true;
        }
    }

    static boolean constantTimeEquals(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }
}
