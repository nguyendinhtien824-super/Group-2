package service;

import exception.EntityNotFoundException;
import exception.InvalidOrderException;
import model.Customer;
import model.FlashItem;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.Voucher;
import model.enums.OrderStatus;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

import java.util.List;

final class OrderLifecycleService {
    private static final String VOUCHER_MARKER = "Voucher: ";

    private final FlashItemRepository flashItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final OrderTransactionRepository transactionRepository;

    OrderLifecycleService(FlashItemRepository flashItemRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          CustomerRepository customerRepository,
                          VoucherRepository voucherRepository,
                          OrderTransactionRepository transactionRepository) {
        this.flashItemRepository = flashItemRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
        this.transactionRepository = transactionRepository;
    }

    boolean approveOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            order.approve();
            orderRepository.save(order);
            OrderContext context = resolveOrderContext(order);
            saveTransaction(order, context, OrderStatus.APPROVED,
                    "Đơn hàng đã được Admin phê duyệt thành công");
            return true;
        }
    }

    boolean completeOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            order.complete();
            orderRepository.save(order);
            OrderContext context = resolveOrderContext(order);
            saveTransaction(order, context, OrderStatus.SUCCESS,
                    "Giao hàng thành công | Đơn hàng hoàn tất");
            return true;
        }
    }

    boolean cancelOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            order.cancel();
            OrderContext context = resolveOrderContext(order);

            restoreCustomerBalance(order);
            restoreInventory(order, context.details());
            restoreVoucher(order.getOrderId());

            orderRepository.save(order);
            saveTransaction(order, context, OrderStatus.CANCELLED,
                    "Đơn hàng đã bị hủy bởi Admin. Tiền ví, tồn kho và voucher đã được hoàn trả.");
            return true;
        }
    }

    void cancelAllPendingAndApprovedOrders(String customerId) {
        synchronized (orderRepository) {
            List<Order> cancellableOrders = orderRepository.findAll().stream()
                    .filter(order -> customerId.equalsIgnoreCase(order.getCustomerId()))
                    .filter(order -> order.getOrderStatus() == OrderStatus.PENDING
                            || order.getOrderStatus() == OrderStatus.APPROVED)
                    .toList();
            for (Order order : cancellableOrders) {
                try {
                    cancelOrder(order.getOrderId());
                } catch (InvalidOrderException e) {
                    throw new IllegalStateException(
                            "Không thể hủy đơn " + order.getOrderId() + " của khách hàng " + customerId, e);
                }
            }
        }
    }

    private Order requireOrder(String orderId) throws EntityNotFoundException {
        if (orderId == null || orderId.isBlank()) {
            throw new EntityNotFoundException("Mã đơn hàng không được để trống");
        }
        Order order = orderRepository.findById(orderId.trim());
        if (order == null) {
            throw new EntityNotFoundException("Đơn hàng không tồn tại.");
        }
        return order;
    }

    private void restoreCustomerBalance(Order order) {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(order.getCustomerId());
            if (customer != null) {
                customer.setWalletBalance(customer.getWalletBalance() + order.getTotalAmount());
                customerRepository.save(customer);
            }
        }
    }

    private void restoreInventory(Order order, List<OrderDetail> details) {
        synchronized (flashItemRepository) {
            for (OrderDetail detail : details) {
                FlashItem item = findItem(order, detail);
                if (item != null) {
                    item.setSoldQty(Math.max(0, item.getSoldQty() - detail.getQuantity()));
                    item.setVersion(item.getVersion() + 1);
                    flashItemRepository.save(item);
                }
            }
        }
    }

    private FlashItem findItem(Order order, OrderDetail detail) {
        return flashItemRepository.findAll().stream()
                .filter(item -> detail.getProductId().equalsIgnoreCase(item.getProductId()))
                .filter(item -> order.getEventId() == null || order.getEventId().isBlank()
                        || order.getEventId().equalsIgnoreCase(item.getEventId()))
                .findFirst()
                .orElse(null);
    }

    private void restoreVoucher(String orderId) {
        OrderTransaction initialTransaction = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equalsIgnoreCase(transaction.getOrderId()))
                .filter(transaction -> OrderStatus.PENDING.name()
                        .equalsIgnoreCase(transaction.getStatus()))
                .findFirst()
                .orElse(null);
        String voucherCode = extractVoucherCode(initialTransaction);
        if (voucherCode == null) {
            return;
        }
        synchronized (voucherRepository) {
            Voucher voucher = voucherRepository.findByCode(voucherCode);
            if (voucher != null) {
                voucher.setRemainingUses(voucher.getRemainingUses() + 1);
                voucherRepository.save(voucher);
            }
        }
    }

    private static String extractVoucherCode(OrderTransaction transaction) {
        if (transaction == null || transaction.getMessage() == null) {
            return null;
        }
        int markerIndex = transaction.getMessage().indexOf(VOUCHER_MARKER);
        if (markerIndex < 0) {
            return null;
        }
        String voucherCode = transaction.getMessage()
                .substring(markerIndex + VOUCHER_MARKER.length()).trim();
        return voucherCode.equalsIgnoreCase("Không dùng") || voucherCode.isEmpty()
                ? null : voucherCode;
    }

    private OrderContext resolveOrderContext(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getOrderId());
        if (details.isEmpty()) {
            return new OrderContext("", 0, details);
        }
        OrderDetail firstDetail = details.get(0);
        FlashItem firstItem = findItem(order, firstDetail);
        int totalQuantity = details.stream().mapToInt(OrderDetail::getQuantity).sum();
        return new OrderContext(firstItem != null ? firstItem.getItemId() : "",
                totalQuantity, details);
    }

    private void saveTransaction(Order order, OrderContext context,
                                 OrderStatus status, String message) {
        transactionRepository.save(new OrderTransaction(
                transactionRepository.generateNewTransactionId(),
                order.getOrderId(), order.getCustomerId(), context.itemId(), context.quantity(),
                status.name(), message, System.currentTimeMillis()));
    }

    private record OrderContext(String itemId, int quantity, List<OrderDetail> details) {
    }
}
