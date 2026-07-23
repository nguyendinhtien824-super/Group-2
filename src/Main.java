import app.FlashSaleApplication;

/** Compatibility entrypoint; the packaged JAR uses FlashSaleApplication directly. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        FlashSaleApplication.main(args);
    }
}
