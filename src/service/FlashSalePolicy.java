package service;

import config.FlashSaleFormats;
import exception.InvalidDiscountException;
import exception.InvalidEventException;
import exception.InvalidOrderException;
import model.FlashItem;
import model.FlashSaleEvent;
import model.enums.CustTier;
import model.enums.SaleStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class FlashSalePolicy {
    private FlashSalePolicy() {
    }

    public static void validateQuantity(int quantity) throws InvalidOrderException {
        if (quantity < 1 || quantity > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new InvalidOrderException(String.format(
                    "Mỗi khách hàng chỉ được mua từ 1 đến %d sản phẩm trong một sự kiện",
                    FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT));
        }
    }

    public static void validateEventSchedule(FlashSaleEvent event) throws InvalidEventException {
        LocalDateTime start = parseEventTime(event.getStartTime(), "bắt đầu");
        LocalDateTime end = parseEventTime(event.getEndTime(), "kết thúc");
        long durationMinutes = Duration.between(start, end).toMinutes();
        if (durationMinutes < FlashSaleConstants.MIN_EVENT_DURATION_MINUTES
                || durationMinutes > FlashSaleConstants.MAX_EVENT_DURATION_MINUTES) {
            throw new InvalidEventException("Sự kiện Flash Sale phải kéo dài từ 1 đến 2 giờ");
        }
    }

    public static void requireBookable(FlashSaleEvent event, LocalDateTime now)
            throws InvalidOrderException {
        SaleStatus status = event.getSaleStatus();
        if (status == SaleStatus.LOCKED) {
            throw new InvalidOrderException(buildLockedMessage(event, now));
        }
        if (status.isEnded()) {
            throw new InvalidOrderException("Sự kiện Flash Sale đã kết thúc");
        }
        if (status != SaleStatus.ACTIVE) {
            throw new InvalidOrderException("Sự kiện Flash Sale hiện không hoạt động");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(event.getStartTime(), FlashSaleFormats.EVENT_TIME);
            end = LocalDateTime.parse(event.getEndTime(), FlashSaleFormats.EVENT_TIME);
        } catch (DateTimeParseException e) {
            throw new InvalidOrderException("Lỗi định dạng thời gian sự kiện Flash Sale");
        }
        if (now.isBefore(start)) {
            throw new InvalidOrderException("Sự kiện Flash Sale chưa bắt đầu");
        }
        if (!now.isBefore(end)) {
            throw new InvalidOrderException("Sự kiện Flash Sale đã kết thúc");
        }
    }

    public static void validateDiscount(FlashItem item) throws InvalidDiscountException {
        if (item == null || item.getOriginalPrice() <= 0 || item.getSalePrice() < 0
                || item.getSalePrice() >= item.getOriginalPrice()) {
            throw new InvalidDiscountException("Giá gốc và giá Flash Sale không hợp lệ");
        }
        double discountPercent = 100.0 * (item.getOriginalPrice() - item.getSalePrice())
                / item.getOriginalPrice();
        if (discountPercent < FlashSaleConstants.MIN_DISCOUNT_PERCENT
                || discountPercent > FlashSaleConstants.MAX_DISCOUNT_PERCENT) {
            throw new InvalidDiscountException("Mức giảm giá Flash Sale phải từ 30% đến 70%");
        }
    }

    public static double tierDiscountRate(CustTier tier) {
        if (tier == null) {
            return 0.0;
        }
        return switch (tier) {
            case SILVER -> FlashSaleConstants.SILVER_DISCOUNT_RATE;
            case GOLD -> FlashSaleConstants.GOLD_DISCOUNT_RATE;
            case DIAMOND -> FlashSaleConstants.DIAMOND_DISCOUNT_RATE;
            case STANDARD -> 0.0;
        };
    }

    private static LocalDateTime parseEventTime(String value, String field)
            throws InvalidEventException {
        try {
            return LocalDateTime.parse(value, FlashSaleFormats.EVENT_TIME);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidEventException("Thời gian " + field + " của sự kiện không hợp lệ", e);
        }
    }

    private static String buildLockedMessage(FlashSaleEvent event, LocalDateTime now) {
        String unlockTime = event.getUnlockTime();
        if (unlockTime == null || unlockTime.isBlank()) {
            return "Sự kiện Flash Sale này đang bị tạm khóa";
        }
        try {
            LocalDateTime unlock = LocalDateTime.parse(
                    unlockTime, FlashSaleFormats.EVENT_TIME);
            if (now.isBefore(unlock)) {
                Duration remaining = Duration.between(now, unlock);
                return String.format(
                        "Sự kiện Flash Sale đang bị tạm khóa. Dự kiến mở lại sau %d phút %d giây (lúc %s).",
                        remaining.toMinutes(), remaining.minusMinutes(remaining.toMinutes()).getSeconds(),
                        unlockTime);
            }
        } catch (DateTimeParseException ignored) {
            return "Sự kiện Flash Sale này đang bị tạm khóa";
        }
        return "Sự kiện Flash Sale này đang bị tạm khóa";
    }
}
