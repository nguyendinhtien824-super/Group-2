package exception;

public class PurchaseLimitExceededException extends InvalidOrderException {
    public PurchaseLimitExceededException(String message) {
        super(message);
    }
}
