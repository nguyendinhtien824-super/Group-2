package exception;

/** Signals that a non-interactive console stream has reached EOF. */
public class EndOfInputException extends RuntimeException {
    public EndOfInputException() {
        super("Console input has ended");
    }
}
