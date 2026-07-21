package repository;

import model.OrderDetail;

import java.util.Collection;
import java.util.List;

/**
 * Repository xử lý I/O dữ liệu chi tiết đơn hàng từ file CSV.
 */
public class OrderDetailRepository extends CsvRepository<OrderDetail> {
    public OrderDetailRepository() {
        super(OrderDetail.class, "order_details.csv", "detailId,orderId,productId,quantity,unitPrice,subtotal");
    }

    public OrderDetailRepository(String dataDirectory) {
        super(OrderDetail.class, dataDirectory, "order_details.csv", "detailId,orderId,productId,quantity,unitPrice,subtotal");
    }

    /**
     * Tự động sinh ID chi tiết đơn hàng mới dạng D-XXXXX dựa vào ID lớn nhất hiện tại.
     */
    public String generateNewDetailId() {
        int maxId = findAll().stream()
                .map(OrderDetail::getDetailId)
                .filter(id -> id != null && id.startsWith("D-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("D-%05d", maxId + 1);
    }

    public List<OrderDetail> findByOrderId(String orderId) {
        return findAll().stream()
                .filter(detail -> orderId.equals(detail.getOrderId()))
                .toList();
    }

    public int totalQuantity(Collection<String> orderIds, String productId) {
        return findAll().stream()
                .filter(detail -> orderIds.contains(detail.getOrderId()))
                .filter(detail -> productId.equals(detail.getProductId()))
                .mapToInt(OrderDetail::getQuantity)
                .sum();
    }
}
