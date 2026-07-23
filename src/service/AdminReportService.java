package service;

import model.Customer;
import model.Order;
import model.OrderTransaction;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import repository.CustomerRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Computes immutable administrator revenue, customer-tier and voucher analytics. */
public class AdminReportService {
    private static final String VOUCHER_MARKER = "Voucher: ";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final OrderTransactionRepository transactionRepository;

    public AdminReportService(OrderRepository orderRepository,
                              CustomerRepository customerRepository,
                              VoucherRepository voucherRepository,
                              OrderTransactionRepository transactionRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.voucherRepository = Objects.requireNonNull(voucherRepository, "voucherRepository");
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository, "transactionRepository");
    }

    public Report generate() {
        List<Order> orders = orderRepository.findAll();
        Map<OrderStatus, Long> orderCounts = countOrdersByStatus(orders);
        Set<String> successfulOrderIds = orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.SUCCESS)
                .map(Order::getOrderId)
                .collect(Collectors.toSet());
        long totalRevenue = orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.SUCCESS)
                .mapToLong(Order::getTotalAmount)
                .sum();

        Map<CustTier, Long> tierCounts = countCustomersByTier(customerRepository.findAll());
        Map<String, Long> successfulVoucherUses = countSuccessfulVoucherUses(
                transactionRepository.findAll(), successfulOrderIds);
        List<VoucherMetric> vouchers = voucherRepository.findAll().stream()
                .sorted((first, second) -> first.getCode().compareToIgnoreCase(second.getCode()))
                .map(voucher -> toMetric(voucher, successfulVoucherUses))
                .toList();

        return new Report(totalRevenue,
                orderCounts.getOrDefault(OrderStatus.SUCCESS, 0L),
                orders.size() - orderCounts.getOrDefault(OrderStatus.SUCCESS, 0L),
                orderCounts, tierCounts, vouchers);
    }

    private static Map<OrderStatus, Long> countOrdersByStatus(List<Order> orders) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Order order : orders) {
            counts.compute(order.getOrderStatus(), (status, count) -> count + 1);
        }
        return Map.copyOf(counts);
    }

    private static Map<CustTier, Long> countCustomersByTier(List<Customer> customers) {
        Map<CustTier, Long> counts = new EnumMap<>(CustTier.class);
        for (CustTier tier : CustTier.values()) {
            counts.put(tier, 0L);
        }
        for (Customer customer : customers) {
            CustTier tier = customer.getTier() == null ? CustTier.STANDARD : customer.getTier();
            counts.compute(tier, (key, count) -> count + 1);
        }
        return Map.copyOf(counts);
    }

    private static Map<String, Long> countSuccessfulVoucherUses(
            List<OrderTransaction> transactions, Set<String> successfulOrderIds) {
        Map<String, String> voucherByOrder = new LinkedHashMap<>();
        for (OrderTransaction transaction : transactions) {
            if (!successfulOrderIds.contains(transaction.getOrderId())
                    || !OrderStatus.PENDING.name().equalsIgnoreCase(transaction.getStatus())) {
                continue;
            }
            String code = extractVoucherCode(transaction.getMessage());
            if (code != null) {
                voucherByOrder.putIfAbsent(transaction.getOrderId(), code);
            }
        }
        Map<String, Long> counts = new HashMap<>();
        voucherByOrder.values().forEach(code -> counts.merge(code, 1L, Long::sum));
        return counts;
    }

    private static String extractVoucherCode(String message) {
        if (message == null) {
            return null;
        }
        int markerIndex = message.indexOf(VOUCHER_MARKER);
        if (markerIndex < 0) {
            return null;
        }
        String code = message.substring(markerIndex + VOUCHER_MARKER.length()).trim();
        if (code.isEmpty() || code.equalsIgnoreCase("Không dùng")) {
            return null;
        }
        return code.toUpperCase(Locale.ROOT);
    }

    private static VoucherMetric toMetric(Voucher voucher, Map<String, Long> successfulUses) {
        return new VoucherMetric(voucher.getVoucherId(), voucher.getCode(), voucher.getType(),
                voucher.getValue(), voucher.getMaxDiscount(), voucher.getMinOrderAmount(),
                voucher.getRemainingUses(), successfulUses.getOrDefault(
                        voucher.getCode().toUpperCase(Locale.ROOT), 0L));
    }

    public record Report(long totalRevenue, long successfulOrders, long otherOrders,
                         Map<OrderStatus, Long> ordersByStatus,
                         Map<CustTier, Long> customersByTier,
                         List<VoucherMetric> vouchers) {
        public Report {
            ordersByStatus = Map.copyOf(ordersByStatus);
            customersByTier = Map.copyOf(customersByTier);
            vouchers = List.copyOf(vouchers);
        }
    }

    public record VoucherMetric(String voucherId, String code, String type, int value,
                                int maxDiscount, int minOrderAmount, int remainingUses,
                                long successfulUses) {
    }
}
