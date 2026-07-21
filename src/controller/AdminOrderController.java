package controller;

import model.Order;
import model.OrderDetail;
import service.AdminOrderService;

import java.util.List;

/** Coordinates admin order queries and state transitions. */
public class AdminOrderController {
    private final AdminOrderService queryService;
    private final OrderController orderController;

    public AdminOrderController(AdminOrderService queryService, OrderController orderController) {
        this.queryService = queryService;
        this.orderController = orderController;
    }

    public List<Order> listOrders() {
        return queryService.listOrders();
    }

    public Order findOrder(String orderId) {
        return queryService.findOrder(orderId);
    }

    public List<OrderDetail> getDetails(String orderId) {
        return queryService.getDetails(orderId);
    }

    public String getProductName(String productId) {
        return queryService.getProductName(productId);
    }

    public boolean approve(String orderId) throws Exception {
        return orderController.approveOrder(orderId);
    }

    public boolean cancel(String orderId) throws Exception {
        return orderController.cancelOrder(orderId);
    }

    public boolean complete(String orderId) throws Exception {
        return orderController.completeOrder(orderId);
    }
}
