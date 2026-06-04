package controller;

import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ theo dõi đơn hàng dành cho khách hàng.
 */
public class OrderTrackingController {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderTransactionRepository transactionRepository;

    public OrderTrackingController(OrderRepository orderRepository,
                                   OrderDetailRepository orderDetailRepository,
                                   OrderTransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Lấy danh sách đơn hàng của khách hàng, sắp xếp mới nhất trước.
     */
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Lấy chi tiết sản phẩm trong một đơn hàng.
     * Trả về null nếu orderId không tồn tại.
     */
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Lấy danh sách chi tiết (sản phẩm, số lượng, giá) của một đơn hàng.
     */
    public List<OrderDetail> getOrderDetails(String orderId) {
        return orderDetailRepository.findByOrderId(orderId);
    }

    /**
     * Lấy lịch sử giao dịch của khách hàng, sắp xếp mới nhất trước.
     */
    public List<OrderTransaction> getTransactionsByCustomer(String customerId) {
        return transactionRepository.findByCustomerId(customerId);
    }

    /**
     * Lấy tất cả giao dịch liên quan đến một đơn hàng cụ thể.
     */
    public List<OrderTransaction> getTransactionsByOrder(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }
}
