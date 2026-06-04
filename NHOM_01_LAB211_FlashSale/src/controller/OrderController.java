package controller;

import service.FlashSaleService;

public class OrderController {
    private final FlashSaleService flashSaleService;

    public OrderController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    public boolean bookItem(String itemId, int quantity, String customerId) throws Exception {
        return flashSaleService.bookItem(itemId, quantity, customerId);
    }

    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode) throws Exception {
        return flashSaleService.bookItem(itemId, quantity, customerId, voucherCode);
    }
}
