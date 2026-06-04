package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;

public interface FlashSaleService {
    boolean bookItem(String itemId, int quantity, String customerId) throws InvalidOrderException, InsufficientStockException;
    boolean bookItem(String itemId, int quantity, String customerId, String voucherCode) throws InvalidOrderException, InsufficientStockException;
}

