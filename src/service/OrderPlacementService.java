package service;

import exception.EntityNotFoundException;
import exception.InsufficientStockException;
import exception.InvalidOrderException;
import exception.PurchaseLimitExceededException;
import model.Customer;
import model.FlashItem;
import model.FlashSaleEvent;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

final class OrderPlacementService {
    private final FlashItemRepository flashItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final OrderTransactionRepository transactionRepository;
    private final FlashSaleEventRepository eventRepository;
    private final OrderRequestQueue requestQueue;

    OrderPlacementService(FlashItemRepository flashItemRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          CustomerRepository customerRepository,
                          VoucherRepository voucherRepository,
                          OrderTransactionRepository transactionRepository,
                          FlashSaleEventRepository eventRepository,
                          OrderRequestQueue requestQueue) {
        this.flashItemRepository = flashItemRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.requestQueue = Objects.requireNonNull(requestQueue, "requestQueue");
    }

    boolean placeOrder(String itemId, int quantity, String customerId, String voucherCode)
            throws InvalidOrderException, InsufficientStockException {
        String normalizedItemId = requireIdentifier(itemId, "Mã sản phẩm (ItemId)");
        String normalizedCustomerId = requireIdentifier(customerId, "Mã khách hàng");
        FlashSalePolicy.validateQuantity(quantity);

        Customer schedulingCustomer = requireCustomer(normalizedCustomerId);
        try (OrderRequestQueue.Permit ignored =
                     requestQueue.acquire(schedulingCustomer.getTier())) {
            synchronized (orderRepository) {
                Customer customer = requireCustomer(normalizedCustomerId);
                FlashItem item = requireItem(normalizedItemId);
                FlashSaleEvent event = requireEvent(item.getEventId());
                FlashSalePolicy.requireBookable(event, LocalDateTime.now());
                enforceCumulativeLimit(customer.getCustomerId(), event.getEventId(),
                        item.getProductId(), quantity);

                int unitPrice = calculateUnitPrice(item, customer.getTier());
                int subtotal = multiplySafely(quantity, unitPrice);
                VoucherQuote voucherQuote = quoteVoucher(voucherCode, subtotal);
                int totalAmount = subtotal - voucherQuote.discountAmount();
                Voucher reservedVoucher = reserveVoucher(voucherQuote.voucher());
                boolean debitCompleted = false;

                try {
                    debitWalletAndSellStock(customer.getCustomerId(), normalizedItemId,
                            quantity, totalAmount);
                    debitCompleted = true;
                    persistOrder(customer, event, item, quantity, unitPrice, subtotal,
                            totalAmount, voucherCode);
                    return true;
                } catch (InvalidOrderException | InsufficientStockException | RuntimeException e) {
                    if (debitCompleted) {
                        rollbackPaymentAndStock(customer.getCustomerId(), normalizedItemId,
                                quantity, totalAmount);
                    }
                    restoreVoucher(reservedVoucher);
                    throw e;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InvalidOrderException("Yêu cầu đặt hàng đã bị gián đoạn");
        }
    }

    private Customer requireCustomer(String customerId) throws InvalidOrderException {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new EntityNotFoundException("Khách hàng không tồn tại");
        }
        if ("BANNED".equalsIgnoreCase(customer.getStatus())) {
            throw new InvalidOrderException(
                    "Tài khoản của bạn đã bị khóa (BANNED). Không thể thực hiện đặt hàng.");
        }
        return customer;
    }

    private FlashItem requireItem(String itemId) throws EntityNotFoundException {
        FlashItem item = flashItemRepository.findById(itemId);
        if (item == null) {
            throw new EntityNotFoundException("Sản phẩm Flash Sale không tồn tại");
        }
        return item;
    }

    private FlashSaleEvent requireEvent(String eventId) throws EntityNotFoundException {
        FlashSaleEvent event = eventRepository.findById(eventId);
        if (event == null) {
            throw new EntityNotFoundException("Sự kiện Flash Sale không tồn tại");
        }
        return event;
    }

    private void enforceCumulativeLimit(String customerId, String eventId,
                                        String productId, int requestedQuantity)
            throws PurchaseLimitExceededException {
        List<String> orderIds = orderRepository
                .findPurchasesCountingTowardLimit(customerId, eventId).stream()
                .map(Order::getOrderId)
                .toList();
        int alreadyBought = orderDetailRepository.totalQuantity(orderIds, productId);
        if (alreadyBought + requestedQuantity
                > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new PurchaseLimitExceededException(String.format(
                    "Bạn đã mua %d sản phẩm này trong sự kiện. Giới hạn tối đa là %d sản phẩm cho mọi hạng.",
                    alreadyBought, FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT));
        }
    }

    private static int calculateUnitPrice(FlashItem item, CustTier tier) {
        return (int) (item.getSalePrice() * (1.0 - FlashSalePolicy.tierDiscountRate(tier)));
    }

    private VoucherQuote quoteVoucher(String voucherCode, int subtotal)
            throws InvalidOrderException {
        if (voucherCode == null || voucherCode.isBlank()) {
            return new VoucherQuote(null, 0);
        }
        Voucher voucher = voucherRepository.findByCode(voucherCode.trim());
        if (voucher == null) {
            throw new InvalidOrderException("Mã voucher không tồn tại");
        }
        if (voucher.getRemainingUses() <= 0) {
            throw new InvalidOrderException("Voucher đã hết lượt sử dụng");
        }
        if (subtotal < voucher.getMinOrderAmount()) {
            throw new InvalidOrderException(String.format(
                    "Giá trị đơn hàng (%d) chưa đạt yêu cầu tối thiểu của voucher (%d)",
                    subtotal, voucher.getMinOrderAmount()));
        }
        int discountAmount = switch (voucher.getType().toUpperCase()) {
            case "PERCENTAGE" -> Math.min(
                    (subtotal * voucher.getValue()) / 100, voucher.getMaxDiscount());
            case "FIXED" -> voucher.getValue();
            default -> throw new InvalidOrderException("Loại voucher không hợp lệ");
        };
        return new VoucherQuote(voucher, Math.min(discountAmount, subtotal));
    }

    private Voucher reserveVoucher(Voucher voucher) throws InvalidOrderException {
        if (voucher == null) {
            return null;
        }
        synchronized (voucherRepository) {
            Voucher current = voucherRepository.findById(voucher.getVoucherId());
            if (current == null || current.getRemainingUses() <= 0) {
                throw new InvalidOrderException(
                        "Voucher đã hết lượt sử dụng do hệ thống xử lý song song");
            }
            current.setRemainingUses(current.getRemainingUses() - 1);
            voucherRepository.save(current);
            return current;
        }
    }

    private void debitWalletAndSellStock(String customerId, String itemId,
                                         int quantity, int totalAmount)
            throws InvalidOrderException, InsufficientStockException {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(customerId);
            if (customer == null) {
                throw new EntityNotFoundException("Khách hàng không tồn tại");
            }
            if (customer.getWalletBalance() < totalAmount) {
                throw new InvalidOrderException(String.format(
                        "Số dư ví không đủ. Số dư hiện có: %,.0f VND. Cần thanh toán: %,d VND",
                        customer.getWalletBalance(), totalAmount));
            }
            if (!flashItemRepository.sellWithOptimisticLock(itemId, quantity)) {
                throw new InsufficientStockException(
                        "Không đủ tồn kho hoặc xung đột phiên bản (version)");
            }
            double previousBalance = customer.getWalletBalance();
            try {
                customer.setWalletBalance(previousBalance - totalAmount);
                customerRepository.save(customer);
            } catch (RuntimeException e) {
                restoreStock(itemId, quantity);
                throw e;
            }
        }
    }

    private void rollbackPaymentAndStock(String customerId, String itemId,
                                         int quantity, int totalAmount) {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(customerId);
            if (customer != null) {
                customer.setWalletBalance(customer.getWalletBalance() + totalAmount);
                customerRepository.save(customer);
            }
        }
        restoreStock(itemId, quantity);
    }

    private void restoreStock(String itemId, int quantity) {
        synchronized (flashItemRepository) {
            FlashItem item = flashItemRepository.findById(itemId);
            if (item != null) {
                item.setSoldQty(Math.max(0, item.getSoldQty() - quantity));
                item.setVersion(item.getVersion() + 1);
                flashItemRepository.save(item);
            }
        }
    }

    private void persistOrder(Customer customer, FlashSaleEvent event, FlashItem item,
                              int quantity, int unitPrice, int subtotal, int totalAmount,
                              String voucherCode) {
        String orderId = orderRepository.generateNewOrderId();
        Order order = new Order(orderId, customer.getCustomerId(), customer.getName(),
                LocalDate.now().toString(), totalAmount, OrderStatus.PENDING, event.getEventId());
        orderRepository.save(order);

        String detailId = orderDetailRepository.generateNewDetailId();
        OrderDetail detail = new OrderDetail(detailId, orderId, item.getProductId(),
                quantity, unitPrice, subtotal);
        orderDetailRepository.save(detail);

        String transactionId = transactionRepository.generateNewTransactionId();
        String voucherLabel = voucherCode == null || voucherCode.isBlank()
                ? "Không dùng" : voucherCode.trim();
        String message = String.format("Đặt hàng thành công | Hạng: %s | SL: %d | Voucher: %s",
                customer.getTier() != null ? customer.getTier().name() : CustTier.STANDARD.name(),
                quantity, voucherLabel);
        transactionRepository.save(new OrderTransaction(
                transactionId, orderId, customer.getCustomerId(), item.getItemId(), quantity,
                OrderStatus.PENDING.name(), message, System.currentTimeMillis()));
    }

    private void restoreVoucher(Voucher voucher) {
        if (voucher == null) {
            return;
        }
        synchronized (voucherRepository) {
            Voucher current = voucherRepository.findById(voucher.getVoucherId());
            if (current != null) {
                current.setRemainingUses(current.getRemainingUses() + 1);
                voucherRepository.save(current);
            }
        }
    }

    private static int multiplySafely(int quantity, int unitPrice) throws InvalidOrderException {
        try {
            return Math.multiplyExact(quantity, unitPrice);
        } catch (ArithmeticException e) {
            throw new InvalidOrderException("Tổng tiền đơn hàng vượt giới hạn cho phép");
        }
    }

    private static String requireIdentifier(String value, String label)
            throws InvalidOrderException {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(label + " không được để trống");
        }
        return value.trim();
    }

    private record VoucherQuote(Voucher voucher, int discountAmount) {
    }
}
