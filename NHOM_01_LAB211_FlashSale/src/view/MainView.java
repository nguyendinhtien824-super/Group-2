package view;

import controller.CustomerController;
import controller.DataController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.OrderTrackingController;
import controller.SimulatorController;
import model.Customer;
import model.SimulationResult;
import model.enums.LockType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainView {
    private final FlashSaleController flashSaleController;
    private final OrderController orderController;
    private final CustomerController customerController;
    private final DataController dataController;
    private final SimulatorController simulatorController;
    private final OrderTrackingController orderTrackingController;
    private final FlashSaleView flashSaleView;
    private final OrderView orderView;
    private final SimulatorView simulatorView;
    private final ReportView reportView;
    private final ConsoleInput input;
    private Customer loggedInCustomer = null;

    private final repository.FlashSaleEventRepository eventRepo;
    private final repository.CustomerRepository customerRepo;
    private final repository.VoucherRepository voucherRepo;
    private final repository.OrderRepository orderRepo;
    private final repository.FlashItemRepository itemRepo;

    public MainView(FlashSaleController flashSaleController,
                    OrderController orderController,
                    CustomerController customerController,
                    DataController dataController,
                    SimulatorController simulatorController,
                    OrderTrackingController orderTrackingController,
                    FlashSaleView flashSaleView,
                    OrderView orderView,
                    SimulatorView simulatorView,
                    ReportView reportView,
                    repository.FlashSaleEventRepository eventRepo,
                    repository.CustomerRepository customerRepo,
                    repository.VoucherRepository voucherRepo,
                    repository.OrderRepository orderRepo,
                    repository.FlashItemRepository itemRepo) {
        this.flashSaleController = flashSaleController;
        this.orderController = orderController;
        this.customerController = customerController;
        this.dataController = dataController;
        this.simulatorController = simulatorController;
        this.orderTrackingController = orderTrackingController;
        this.flashSaleView = flashSaleView;
        this.orderView = orderView;
        this.simulatorView = simulatorView;
        this.reportView = reportView;
        this.eventRepo = eventRepo;
        this.customerRepo = customerRepo;
        this.voucherRepo = voucherRepo;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.input = new ConsoleInput(new Scanner(System.in));
    }

    public void display() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = input.readInt("Chon chuc nang", 0);
            switch (choice) {
                case 1:
                    generateData();
                    break;
                case 2:
                    flashSaleView.displayItems(flashSaleController.getFlashSaleItems(20));
                    break;
                case 3:
                    bookFlashSaleItem();
                    break;
                case 4:
                    runAllSimulations();
                    break;
                case 5:
                    runSingleSimulation();
                    break;
                case 6:
                    registerCustomer();
                    break;
                case 7:
                    loginCustomer();
                    break;
                case 8:
                    runBenchmark();
                    break;
                case 9:
                    new AdminView(eventRepo, itemRepo, orderRepo, customerRepo, voucherRepo).display();
                    break;
                case 10:
                    new ResearcherView(simulatorController, simulatorView).display();
                    break;
                case 11:
                    trackMyOrders();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
        System.out.println("Da thoat chuong trinh.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("===== LAB211 FLASH SALE CONSOLE =====");
        if (this.loggedInCustomer != null) {
            System.out.println("Khach hang: " + this.loggedInCustomer.getName() + " (" + this.loggedInCustomer.getCustomerId() + ") - Hang: " + this.loggedInCustomer.getTier());
        } else {
            System.out.println("Khach hang: Chua dang nhap");
        }
        System.out.println("1. Tao du lieu CSV theo de bai");
        System.out.println("2. Xem 20 san pham Flash Sale");
        System.out.println("3. Dat hang Flash Sale (Optimistic Lock + Voucher + Tier)");
        System.out.println("4. Chay simulator tat ca co che");
        System.out.println("5. Chay simulator mot co che");
        System.out.println("6. Dang ky khach hang");
        System.out.println("7. Dang nhap khach hang");
        System.out.println("8. Benchmark 3 lan lay trung binh");
        System.out.println("9. Menu Admin (Tao Event/Flash Item, xem Doanh thu/Voucher)");
        System.out.println("10. Menu Researcher (Cau hinh & Chay gia lap nang cao)");
        if (this.loggedInCustomer != null) {
            System.out.println("11. Theo doi don hang cua toi");
        }
        System.out.println("0. Thoat");
    }

    private void generateData() {
        try {
            Map<String, Integer> result = dataController.generateData();
            reportView.displayDataGenerationResult(result);
        } catch (IOException e) {
            System.out.println("Khong tao duoc du lieu CSV: " + e.getMessage());
        }
    }

    private void bookFlashSaleItem() {
        if (this.loggedInCustomer == null) {
            System.out.println("Vui long dang nhap truoc khi dat hang.");
            return;
        }
        model.enums.CustTier tier = this.loggedInCustomer.getTier() != null ? this.loggedInCustomer.getTier() : model.enums.CustTier.STANDARD;
        int maxQty = 1;
        switch (tier) {
            case DIAMOND: maxQty = 3; break;
            case GOLD:
            case SILVER: maxQty = 2; break;
            default: maxQty = 1; break;
        }

        String itemId = input.readLine("Nhap itemId: ");
        int quantity = input.readInt("Nhap so luong (Hang " + tier + " toi da " + maxQty + ")", 1);
        String voucherCode = input.readLine("Nhap ma voucher (De trong neu khong co): ");
        if (voucherCode != null && voucherCode.trim().isEmpty()) {
            voucherCode = null;
        }

        try {
            boolean success = orderController.bookItem(itemId, quantity, this.loggedInCustomer.getCustomerId(), voucherCode);
            orderView.displayBookingResult(success);
            // Reload loggedInCustomer to refresh if data changed
            this.loggedInCustomer = customerRepo.findById(this.loggedInCustomer.getCustomerId());
        } catch (Exception e) {
            orderView.displayBookingError(e);
        }
    }

    private void runAllSimulations() {
        int threads = input.readInt("So thread", 500);
        int stock = input.readInt("Ton kho ban dau", 100);
        List<SimulationResult> results = simulatorController.runAll(threads, stock);
        simulatorView.displayResults(results);
    }

    private void runSingleSimulation() {
        LockType lockType = input.readLockType("Nhap co che", LockType.OPTIMISTIC_LOCK);
        int threads = input.readInt("So thread", 500);
        int stock = input.readInt("Ton kho ban dau", 100);
        simulatorView.displayResults(List.of(simulatorController.runSingle(lockType, threads, stock)));
    }

    private void runBenchmark() {
        int threads = input.readInt("So thread", 1000);
        int stock = input.readInt("Ton kho ban dau", 100);
        int repeats = input.readInt("So lan lap", 3);
        simulatorView.displayResults(simulatorController.runBenchmark(threads, stock, repeats));
    }

    private void registerCustomer() {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", input.readLine("Ho ten: "));
        payload.put("email", input.readLine("Email: "));
        payload.put("phone", input.readLine("So dien thoai: "));
        payload.put("address", input.readLine("Dia chi: "));

        Map<String, Object> result = customerController.register(payload);
        System.out.println(result.get("message"));
    }

    private void loginCustomer() {
        Map<String, String> payload = Map.of("email", input.readLine("Email: "));
        Customer customer = customerController.login(payload).orElse(null);
        if (customer == null) {
            System.out.println("Dang nhap that bai.");
            this.loggedInCustomer = null;
            return;
        }

        if ("BANNED".equalsIgnoreCase(customer.getStatus())) {
            System.out.println("Tai khoan cua ban da bi khoa (BANNED). Khong the dang nhap.");
            this.loggedInCustomer = null;
            return;
        }

        this.loggedInCustomer = customer;
        System.out.println("Dang nhap thanh cong: " + customer.getName() + " (" + customer.getCustomerId() + ")");
    }

    private void trackMyOrders() {
        if (this.loggedInCustomer == null) {
            System.out.println("Vui long dang nhap truoc khi xem don hang.");
            return;
        }
        new OrderTrackingView(orderTrackingController).display(this.loggedInCustomer.getCustomerId());
    }
}

