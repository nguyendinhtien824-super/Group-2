package repository;

import model.Order;

import java.util.List;
import java.util.stream.Collectors;

public class OrderRepository extends CsvRepository<Order> {
    public OrderRepository() {
        super("orders.csv", "orderId,customerId,orderDate,totalAmount,status");
    }

    public OrderRepository(String dataDirectory) {
        super(dataDirectory, "orders.csv", "orderId,customerId,orderDate,totalAmount,status");
    }

    @Override
    protected Order parseLine(String line) {
        return Order.fromCsvLine(line);
    }

    /**
     * Lấy danh sách tất cả đơn hàng của một khách hàng, sắp xếp mới nhất lên trên.
     */
    public List<Order> findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .collect(Collectors.toList());
    }
}

