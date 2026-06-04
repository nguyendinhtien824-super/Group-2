package view;

import model.FlashItem;
import model.FlashSaleEvent;
import model.enums.CustTier;
import model.Order;
import model.Voucher;
import repository.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

public class AdminView {
    private final FlashSaleEventRepository eventRepo;
    private final FlashItemRepository itemRepo;
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final VoucherRepository voucherRepo;
    private final ConsoleInput input;

    public AdminView(FlashSaleEventRepository eventRepo,
                     FlashItemRepository itemRepo,
                     OrderRepository orderRepo,
                     CustomerRepository customerRepo,
                     VoucherRepository voucherRepo) {
        this.eventRepo = eventRepo;
        this.itemRepo = itemRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.voucherRepo = voucherRepo;
        this.input = new ConsoleInput(new Scanner(System.in));
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("===== MENU QUAN TRI VIEN (ADMIN) =====");
            System.out.println("1. Tao su kien Flash Sale moi");
            System.out.println("2. Tao san pham Flash Sale moi");
            System.out.println("3. Xem bao cao doanh thu & Phan tich Voucher");
            System.out.println("4. Quan ly tai khoan khach hang (CRUD + Ban/Unban)");
            System.out.println("0. Quay lai menu chinh");
            int choice = input.readInt("Nhap lua chon cua ban", 0);
            switch (choice) {
                case 1:
                    createEvent();
                    break;
                case 2:
                    createFlashItem();
                    break;
                case 3:
                    showRevenueAndAnalytics();
                    break;
                case 4:
                    new AdminCustomerView(customerRepo).display();
                    break;
                case 0:
                    back = true;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void createEvent() {
        System.out.println("\n--- TAO SU KIEN FLASH SALE MOI ---");
        String eventId = input.readLine("Nhap ma su kien (VD: EV-100): ");
        if (eventRepo.findById(eventId) != null) {
            System.out.println("Ma su kien da ton tai.");
            return;
        }
        String name = input.readLine("Nhap ten su kien: ");
        String startTime = input.readLine("Nhap thoi gian bat dau (YYYY-MM-DD HH:MM:SS): ");
        String endTime = input.readLine("Nhap thoi gian ket thuc (YYYY-MM-DD HH:MM:SS): ");
        
        FlashSaleEvent newEvent = new FlashSaleEvent(eventId, name, startTime, endTime, "ACTIVE");
        eventRepo.save(newEvent);
        System.out.println("Tao su kien thanh cong va luu vao CSV.");
    }

    private void createFlashItem() {
        System.out.println("\n--- TAO SAN PHAM FLASH SALE MOI ---");
        String itemId = input.readLine("Nhap ma san pham Flash Sale (VD: FI-10001): ");
        if (itemRepo.findById(itemId) != null) {
            System.out.println("Ma san pham Flash Sale da ton tai.");
            return;
        }
        String productId = input.readLine("Nhap ma san pham goc (VD: P-00001): ");
        String eventId = input.readLine("Nhap ma su kien lien ket (VD: EV-001): ");
        if (eventRepo.findById(eventId) == null) {
            System.out.println("Canh bao: Su kien lien ket khong ton tai trong he thong.");
        }
        String productName = input.readLine("Nhap ten san pham: ");
        int originalPrice = input.readInt("Nhap gia goc: ", 0);
        int salePrice = input.readInt("Nhap gia ban flash sale: ", 0);
        int initialStock = input.readInt("Nhap ton kho ban dau: ", 0);

        FlashItem newItem = new FlashItem(itemId, productId, eventId, productName, originalPrice, salePrice, initialStock);
        itemRepo.save(newItem);
        System.out.println("Tao san pham Flash Sale thanh cong va luu vao CSV.");
    }

    private void showRevenueAndAnalytics() {
        System.out.println("\n--- BAO CAO DOANH THU & PHAN TICH VOUCHER ---");
        List<Order> orders = orderRepo.findAll();
        
        long totalRevenue = 0;
        int successCount = 0;
        int failedCount = 0;
        
        for (Order o : orders) {
            if ("SUCCESS".equalsIgnoreCase(o.getStatus())) {
                totalRevenue += o.getTotalAmount();
                successCount++;
            } else {
                failedCount++;
            }
        }
        
        System.out.println("Tong doanh thu cac don hang thanh cong: " + totalRevenue + " VND");
        System.out.println("So don hang thanh cong: " + successCount);
        System.out.println("So don hang that bai: " + failedCount);
        
        System.out.println("\n--- Thong ke Voucher trong he thong ---");
        List<Voucher> vouchers = voucherRepo.findAll();
        System.out.printf("%-10s | %-12s | %-12s | %-10s | %-15s%n", "Ma Voucher", "Loai", "Gia tri", "Con lai", "Yeu cau toi thieu");
        System.out.println("----------------------------------------------------------------------");
        for (Voucher v : vouchers) {
            System.out.printf("%-10s | %-12s | %-12d | %-10d | %-15d%n", 
                    v.getCode(), v.getType(), v.getValue(), v.getRemainingUses(), v.getMinOrderAmount());
        }
        
        System.out.println("\n--- Thong ke khach hang theo hang thanh vien ---");
        Map<CustTier, Integer> tierCounts = new HashMap<>();
        for (CustTier t : CustTier.values()) {
            tierCounts.put(t, 0);
        }
        
        customerRepo.findAll().forEach(c -> {
            CustTier t = c.getTier() != null ? c.getTier() : CustTier.STANDARD;
            tierCounts.put(t, tierCounts.get(t) + 1);
        });
        
        for (Map.Entry<CustTier, Integer> entry : tierCounts.entrySet()) {
            System.out.println("Hang " + entry.getKey() + ": " + entry.getValue() + " khach hang");
        }
    }
}
