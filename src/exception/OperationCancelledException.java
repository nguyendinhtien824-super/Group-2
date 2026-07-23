package exception;

public class OperationCancelledException extends RuntimeException {
    public OperationCancelledException() {
        super("Thao tác đã bị hủy.");
    }
}
