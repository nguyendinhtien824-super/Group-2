package repository;

import model.OrderDetail;

import java.util.List;
import java.util.stream.Collectors;

public class OrderDetailRepository extends CsvRepository<OrderDetail> {
    public OrderDetailRepository() {
        super("order_details.csv", "detailId,orderId,productId,quantity,unitPrice,subtotal");
    }

    public OrderDetailRepository(String dataDirectory) {
        super(dataDirectory, "order_details.csv", "detailId,orderId,productId,quantity,unitPrice,subtotal");
    }

    @Override
    protected OrderDetail parseLine(String line) {
        return OrderDetail.fromCsvLine(line);
    }

    /**
     * Lấy danh sách chi tiết sản phẩm trong một đơn hàng.
     */
    public List<OrderDetail> findByOrderId(String orderId) {
        return findAll().stream()
                .filter(d -> orderId.equals(d.getOrderId()))
                .collect(Collectors.toList());
    }
}

