package service;

import model.Order;
import model.OrderDetail;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;

import java.util.List;

/** Read model for the admin order console. */
public class AdminOrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository detailRepository;
    private final ProductRepository productRepository;

    public AdminOrderService(OrderRepository orderRepository,
                             OrderDetailRepository detailRepository,
                             ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.detailRepository = detailRepository;
        this.productRepository = productRepository;
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    public Order findOrder(String orderId) {
        return orderRepository.findById(orderId);
    }

    public List<OrderDetail> getDetails(String orderId) {
        return detailRepository.findByOrderId(orderId);
    }

    public String getProductName(String productId) {
        var product = productRepository.findById(productId);
        return product == null ? "Không rõ sản phẩm" : product.getName();
    }
}
