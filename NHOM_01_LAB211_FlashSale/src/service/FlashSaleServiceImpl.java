package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;
import model.Customer;
import model.FlashItem;
import model.Order;
import model.OrderDetail;
import model.Voucher;
import model.enums.CustTier;
import repository.CustomerRepository;
import repository.FlashItemRepository;
import repository.OrderRepository;
import repository.OrderDetailRepository;
import repository.VoucherRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashItemRepository flashItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;

    public FlashSaleServiceImpl(FlashItemRepository flashItemRepository,
                                OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                CustomerRepository customerRepository,
                                VoucherRepository voucherRepository) {
        this.flashItemRepository = flashItemRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
    }

    @Override
    public boolean bookItem(String itemId, int quantity, String customerId) throws InvalidOrderException, InsufficientStockException {
        return bookItem(itemId, quantity, customerId, null);
    }

    @Override
    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode) throws InvalidOrderException, InsufficientStockException {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new InvalidOrderException("ItemId khong duoc de trong");
        }

        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new InvalidOrderException("Khach hang khong ton tai");
        }

        if ("BANNED".equalsIgnoreCase(customer.getStatus())) {
            throw new InvalidOrderException("Tai khoan cua ban da bi khoa (BANNED). Khong the thuc hien dat hang.");
        }

        CustTier tier = customer.getTier() != null ? customer.getTier() : CustTier.STANDARD;

        // 1. Giới hạn số lượng mua mỗi lượt theo hạng thành viên (Shopee Tier Benefits)
        int maxQty = 1;
        switch (tier) {
            case DIAMOND: maxQty = 3; break;
            case GOLD:
            case SILVER: maxQty = 2; break;
            case STANDARD:
            default: maxQty = 1; break;
        }

        if (quantity <= 0 || quantity > maxQty) {
            throw new InvalidOrderException(String.format("Hang %s chi duoc dat mua tu 1 den %d san pham moi luot", tier.name(), maxQty));
        }

        FlashItem item = flashItemRepository.findById(itemId.trim());
        if (item == null) {
            throw new InvalidOrderException("San pham Flash Sale khong ton tai");
        }

        String productId = item.getProductId();
        String eventId = item.getEventId();

        // Tìm tất cả đơn hàng thành công của khách hàng này
        List<Order> customerOrders = orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId().equals(customerId) && "SUCCESS".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.toList());

        List<String> orderIds = customerOrders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        // Tìm tất cả chi tiết đơn hàng của sản phẩm này trong các đơn hàng trên
        List<OrderDetail> customerOrderDetails = orderDetailRepository.findAll().stream()
                .filter(od -> orderIds.contains(od.getOrderId()) && od.getProductId().equals(productId))
                .collect(Collectors.toList());

        int alreadyBought = customerOrderDetails.stream()
                .mapToInt(OrderDetail::getQuantity)
                .sum();

        // Giới hạn tổng sản phẩm tối đa được mua trong sự kiện theo hạng thành viên
        int maxBoughtLimit = (tier == CustTier.DIAMOND) ? 3 : 2;
        if (alreadyBought + quantity > maxBoughtLimit) {
            throw new InvalidOrderException(String.format(
                    "Ban da mua %d san pham nay trong su kien. Gioi han tối đa hang %s la %d san pham.", 
                    alreadyBought, tier.name(), maxBoughtLimit
            ));
        }

        // 2. Tính đơn giá sau khi chiết khấu theo hạng thành viên (Shopee Tier Discount)
        double tierDiscount = 0.0;
        switch (tier) {
            case DIAMOND: tierDiscount = 0.10; break; // Giảm 10%
            case GOLD: tierDiscount = 0.05; break;    // Giảm 5%
            case SILVER: tierDiscount = 0.02; break;  // Giảm 2%
            default: tierDiscount = 0.00; break;
        }

        int unitPrice = (int) (item.getSalePrice() * (1.0 - tierDiscount));
        int subtotal = quantity * unitPrice;

        // 3. Áp dụng mã Voucher (Shopee Voucher System)
        int discountAmount = 0;
        Voucher appliedVoucher = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            String cleanCode = voucherCode.trim();
            appliedVoucher = voucherRepository.findAll().stream()
                    .filter(v -> v.getCode().equalsIgnoreCase(cleanCode))
                    .findFirst()
                    .orElse(null);
            
            if (appliedVoucher == null) {
                throw new InvalidOrderException("Ma voucher khong ton tai");
            }
            if (appliedVoucher.getRemainingUses() <= 0) {
                throw new InvalidOrderException("Voucher da het luot su dung");
            }
            if (subtotal < appliedVoucher.getMinOrderAmount()) {
                throw new InvalidOrderException(String.format(
                        "Gia tri don hang (%d) chua dat yeu cau toi thieu cua voucher (%d)",
                        subtotal, appliedVoucher.getMinOrderAmount()
                ));
            }
            
            if ("PERCENTAGE".equalsIgnoreCase(appliedVoucher.getType())) {
                discountAmount = (subtotal * appliedVoucher.getValue()) / 100;
                if (discountAmount > appliedVoucher.getMaxDiscount()) {
                    discountAmount = appliedVoucher.getMaxDiscount();
                }
            } else if ("FIXED".equalsIgnoreCase(appliedVoucher.getType())) {
                discountAmount = appliedVoucher.getValue();
            }
            discountAmount = Math.min(discountAmount, subtotal);
        }

        // 4. Trừ số lượt dùng của Voucher một cách thread-safe
        if (appliedVoucher != null) {
            synchronized (voucherRepository) {
                Voucher reloaded = voucherRepository.findById(appliedVoucher.getVoucherId());
                if (reloaded == null || reloaded.getRemainingUses() <= 0) {
                    throw new InvalidOrderException("Voucher da het luot su dung do he thong xu ly song song");
                }
                reloaded.setRemainingUses(reloaded.getRemainingUses() - 1);
                voucherRepository.save(reloaded);
            }
        }

        // 5. Thực hiện trừ kho an toàn
        if (!flashItemRepository.sellWithOptimisticLock(itemId.trim(), quantity)) {
            // Revert voucher count if transaction fails
            if (appliedVoucher != null) {
                synchronized (voucherRepository) {
                    Voucher reloaded = voucherRepository.findById(appliedVoucher.getVoucherId());
                    if (reloaded != null) {
                        reloaded.setRemainingUses(reloaded.getRemainingUses() + 1);
                        voucherRepository.save(reloaded);
                    }
                }
            }
            throw new InsufficientStockException("Khong du ton kho hoac xung dot version");
        }

        // 6. Tạo đơn hàng và chi tiết đơn hàng tương ứng
        String newOrderId = String.format("O-%06d", orderRepository.findAll().size() + 1);
        int totalAmount = subtotal - discountAmount;
        Order newOrder = new Order(newOrderId, customerId, LocalDate.now().toString(), totalAmount, "SUCCESS");
        orderRepository.save(newOrder);

        String newDetailId = String.format("D-%06d", orderDetailRepository.findAll().size() + 1);
        OrderDetail newOrderDetail = new OrderDetail(newDetailId, newOrderId, productId, quantity, unitPrice, subtotal);
        orderDetailRepository.save(newOrderDetail);

        return true;
    }
}

