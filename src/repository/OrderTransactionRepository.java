package repository;

import model.OrderTransaction;

/**
 * Repository xử lý I/O dữ liệu lịch sử giao dịch mô phỏng từ file CSV.
 */
public class OrderTransactionRepository extends CsvRepository<OrderTransaction> {
    public OrderTransactionRepository() {
        super(OrderTransaction.class, "transactions.csv", "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }

    public OrderTransactionRepository(String dataDirectory) {
        super(OrderTransaction.class, dataDirectory, "transactions.csv", "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }

    /**
     * Tự động sinh ID giao dịch mới dạng TX-XXXXX dựa vào ID lớn nhất hiện tại.
     */
    public String generateNewTransactionId() {
        int maxId = findAll().stream()
                .map(OrderTransaction::getTransactionId)
                .filter(id -> id != null && id.startsWith("TX-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(3));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("TX-%05d", maxId + 1);
    }
}
