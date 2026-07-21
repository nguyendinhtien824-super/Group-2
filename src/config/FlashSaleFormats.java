package config;

import java.time.format.DateTimeFormatter;

public final class FlashSaleFormats {
    public static final DateTimeFormatter EVENT_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FlashSaleFormats() {
    }
}
