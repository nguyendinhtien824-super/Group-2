package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;
import model.Customer;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

public class FlashSaleServiceImpl implements FlashSaleService {
    private final OrderPlacementService placementService;
    private final OrderLifecycleService lifecycleService;

    public FlashSaleServiceImpl(FlashItemRepository flashItemRepository,
                                OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                CustomerRepository customerRepository,
                                VoucherRepository voucherRepository,
                                OrderTransactionRepository transactionRepository,
                                FlashSaleEventRepository eventRepository) {
        this(flashItemRepository, orderRepository, orderDetailRepository,
                customerRepository, voucherRepository, transactionRepository,
                eventRepository, new OrderRequestQueue());
    }

    public FlashSaleServiceImpl(FlashItemRepository flashItemRepository,
                                OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                CustomerRepository customerRepository,
                                VoucherRepository voucherRepository,
                                OrderTransactionRepository transactionRepository,
                                FlashSaleEventRepository eventRepository,
                                OrderRequestQueue requestQueue) {
        this.placementService = new OrderPlacementService(
                flashItemRepository, orderRepository, orderDetailRepository,
                customerRepository, voucherRepository, transactionRepository,
                eventRepository, requestQueue);
        this.lifecycleService = new OrderLifecycleService(
                flashItemRepository, orderRepository, orderDetailRepository,
                customerRepository, voucherRepository, transactionRepository);
        reconcileBannedCustomers(customerRepository);
    }

    @Override
    public boolean bookItem(String itemId, int quantity, String customerId)
            throws InvalidOrderException, InsufficientStockException {
        return bookItem(itemId, quantity, customerId, null);
    }

    @Override
    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode)
            throws InvalidOrderException, InsufficientStockException {
        return placementService.placeOrder(itemId, quantity, customerId, voucherCode);
    }

    @Override
    public boolean approveOrder(String orderId) throws InvalidOrderException {
        return lifecycleService.approveOrder(orderId);
    }

    @Override
    public boolean cancelOrder(String orderId) throws InvalidOrderException {
        return lifecycleService.cancelOrder(orderId);
    }

    @Override
    public boolean completeOrder(String orderId) throws InvalidOrderException {
        return lifecycleService.completeOrder(orderId);
    }

    @Override
    public void cancelAllPendingAndApprovedOrders(String customerId) {
        lifecycleService.cancelAllPendingAndApprovedOrders(customerId);
    }

    private void reconcileBannedCustomers(CustomerRepository customerRepository) {
        customerRepository.findAll().stream()
                .filter(customer -> "BANNED".equalsIgnoreCase(customer.getStatus()))
                .map(Customer::getCustomerId)
                .forEach(lifecycleService::cancelAllPendingAndApprovedOrders);
    }
}
