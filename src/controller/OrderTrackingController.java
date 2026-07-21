package controller;

import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.FlashItemRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller xử lý các nghiệp vụ theo dõi đơn hàng dành cho khách hàng.
 */
public class OrderTrackingController {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final FlashItemRepository flashItemRepository;

    public OrderTrackingController(OrderRepository orderRepository,
                                   OrderDetailRepository orderDetailRepository,
                                   OrderTransactionRepository transactionRepository) {
        this(orderRepository, orderDetailRepository, transactionRepository,
                new ProductRepository(), new FlashItemRepository());
    }

    public OrderTrackingController(OrderRepository orderRepository,
                                   OrderDetailRepository orderDetailRepository,
                                   OrderTransactionRepository transactionRepository,
                                   ProductRepository productRepository,
                                   FlashItemRepository flashItemRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.flashItemRepository = flashItemRepository;
    }

    /**
     * Lấy danh sách đơn hàng của khách hàng, sắp xếp mới nhất trước.
     */
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId() != null && o.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin một đơn hàng theo ID.
     * Trả về null nếu orderId không tồn tại.
     */
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Lấy danh sách chi tiết (sản phẩm, số lượng, giá) của một đơn hàng.
     */
    public List<OrderDetail> getOrderDetails(String orderId) {
        return orderDetailRepository.findAll().stream()
                .filter(d -> d.getOrderId() != null && d.getOrderId().equals(orderId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử giao dịch của khách hàng, sắp xếp mới nhất trước.
     */
    public List<OrderTransaction> getTransactionsByCustomer(String customerId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getCustomerId() != null && t.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả giao dịch liên quan đến một đơn hàng cụ thể.
     */
    public List<OrderTransaction> getTransactionsByOrder(String orderId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getOrderId() != null && t.getOrderId().equals(orderId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tên sản phẩm từ productId.
     */
    public String getProductName(String productId) {
        model.Product p = productRepository.findById(productId);
        return p != null ? p.getName() : "Không rõ sản phẩm";
    }

    /**
     * Lấy thông tin FlashItem từ productId.
     */
    public model.FlashItem getFlashItemByProductId(String productId) {
        return flashItemRepository.findAll().stream()
                .filter(f -> f.getProductId().equalsIgnoreCase(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Lấy thông tin FlashItem từ itemId.
     */
    public model.FlashItem getFlashItemByItemId(String itemId) {
        return flashItemRepository.findById(itemId);
    }
}
