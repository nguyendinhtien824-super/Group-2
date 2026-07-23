package repository;

import model.FlashItem;

/** Repository boundary validation for Flash Sale inventory invariants. */
final class FlashItemValidator {
    private FlashItemValidator() {
    }

    static void validate(FlashItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Flash Sale item must not be null");
        }
        if (isBlank(item.getItemId()) || isBlank(item.getProductId())
                || isBlank(item.getEventId()) || isBlank(item.getProductName())) {
            throw new IllegalArgumentException("Mã item, sản phẩm, sự kiện và tên không được trống");
        }
        if (item.getOriginalPrice() <= 0 || item.getSalePrice() < 0
                || item.getSalePrice() >= item.getOriginalPrice()) {
            throw new IllegalArgumentException("Giá Flash Sale không hợp lệ");
        }
        if (item.getInitialStock() < 0 || item.getSoldQty() < 0
                || item.getSoldQty() > item.getInitialStock()) {
            throw new IllegalArgumentException("soldQty phải nằm trong khoảng 0..limitedQty");
        }
        int expectedRemaining = item.getInitialStock() - item.getSoldQty();
        if (item.getRemainingStock() != expectedRemaining || item.getVersion() < 0) {
            throw new IllegalArgumentException("Tồn kho hoặc version không đồng bộ");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
