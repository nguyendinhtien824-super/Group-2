package model.enums;

public enum OrderStatus {
    PENDING,
    APPROVED,
    SUCCESS,
    CANCELLED,
    FAILED,
    OUT_OF_STOCK,
    VERSION_CONFLICT;

    public static OrderStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        return OrderStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean countsTowardPurchaseLimit() {
        return this == PENDING || this == APPROVED || this == SUCCESS;
    }
}

