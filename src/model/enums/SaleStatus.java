package model.enums;

public enum SaleStatus {
    UPCOMING,
    ACTIVE,
    LOCKED,
    INACTIVE,
    ENDED,
    EXPIRED;

    public static SaleStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UPCOMING;
        }
        return SaleStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean canStart() {
        return this == UPCOMING || this == INACTIVE;
    }

    public boolean canEnd() {
        return this == ACTIVE || this == LOCKED;
    }

    public boolean isEnded() {
        return this == ENDED || this == EXPIRED;
    }
}

