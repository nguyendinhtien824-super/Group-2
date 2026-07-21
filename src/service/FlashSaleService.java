package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;

public interface FlashSaleService {
    boolean bookItem(String itemId, int quantity, String customerId) throws InvalidOrderException, InsufficientStockException;
    boolean bookItem(String itemId, int quantity, String customerId, String voucherCode) throws InvalidOrderException, InsufficientStockException;
    boolean approveOrder(String orderId) throws InvalidOrderException;
    boolean cancelOrder(String orderId) throws InvalidOrderException;
    boolean completeOrder(String orderId) throws InvalidOrderException;
    void cancelAllPendingAndApprovedOrders(String customerId);
}

