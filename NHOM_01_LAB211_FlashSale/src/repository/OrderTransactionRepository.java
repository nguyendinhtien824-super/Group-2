package repository;

import model.OrderTransaction;

import java.util.List;
import java.util.stream.Collectors;

public class OrderTransactionRepository extends CsvRepository<OrderTransaction> {
    public OrderTransactionRepository() {
        super("transactions.csv", "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }

    public OrderTransactionRepository(String dataDirectory) {
        super(dataDirectory, "transactions.csv", "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }

    @Override
    protected OrderTransaction parseLine(String line) {
        return OrderTransaction.fromCsvLine(line);
    }

    /**
     * Lấy tất cả giao dịch của một khách hàng, sắp xếp mới nhất lên trên.
     */
    public List<OrderTransaction> findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(t -> customerId.equals(t.getCustomerId()))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả giao dịch liên quan đến một đơn hàng.
     */
    public List<OrderTransaction> findByOrderId(String orderId) {
        return findAll().stream()
                .filter(t -> orderId.equals(t.getOrderId()))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }
}

