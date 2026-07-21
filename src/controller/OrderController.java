package controller;

import exception.InsufficientStockException;
import exception.InvalidOrderException;
import service.FlashSaleService;

public class OrderController {
    private final FlashSaleService flashSaleService;

    public OrderController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    public boolean bookItem(String itemId, int quantity, String customerId)
            throws InvalidOrderException, InsufficientStockException {
        return flashSaleService.bookItem(itemId, quantity, customerId);
    }

    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode)
            throws InvalidOrderException, InsufficientStockException {
        return flashSaleService.bookItem(itemId, quantity, customerId, voucherCode);
    }

    public boolean approveOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.approveOrder(orderId);
    }

    public boolean cancelOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.cancelOrder(orderId);
    }

    public boolean completeOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.completeOrder(orderId);
    }

    public void cancelAllPendingAndApprovedOrders(String customerId) {
        flashSaleService.cancelAllPendingAndApprovedOrders(customerId);
    }
}

// Member 3
