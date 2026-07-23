package security;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable admin credentials loaded from process environment variables. */
public final class AdminCredentials {
    private final String username;
    private final String passwordHash;

    private AdminCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static Optional<AdminCredentials> fromEnvironment() {
        return from(System.getenv());
    }

    public static Optional<AdminCredentials> from(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        String username = normalized(environment.get(SecurityEnvironment.ADMIN_USERNAME));
        String passwordHash = normalized(environment.get(SecurityEnvironment.ADMIN_PASSWORD_HASH));

        if (username == null && passwordHash == null) {
            return Optional.empty();
        }
        if (username == null || passwordHash == null) {
            throw new IllegalStateException(
                    SecurityEnvironment.ADMIN_USERNAME + " and "
                            + SecurityEnvironment.ADMIN_PASSWORD_HASH + " must be configured together");
        }
        if (!PasswordSecurity.isEncodedHash(passwordHash)) {
            throw new IllegalStateException(
                    SecurityEnvironment.ADMIN_PASSWORD_HASH + " must contain an Argon2id PHC hash");
        }
        return Optional.of(new AdminCredentials(username, passwordHash));
    }

    public boolean authenticate(String suppliedUsername, String suppliedPassword) {
        boolean passwordMatches = PasswordSecurity.matches(suppliedPassword, passwordHash);
        return PasswordSecurity.constantTimeEquals(username, suppliedUsername) && passwordMatches;
    }

    public String username() {
        return username;
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
