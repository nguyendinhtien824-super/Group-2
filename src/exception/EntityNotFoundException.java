package exception;

public class EntityNotFoundException extends InvalidOrderException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
