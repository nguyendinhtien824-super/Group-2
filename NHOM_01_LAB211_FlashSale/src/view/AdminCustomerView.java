package view;

import model.Customer;
import model.enums.CustTier;
import repository.CustomerRepository;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdminCustomerView {
    private final CustomerRepository customerRepo;
    private final ConsoleInput input;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,11}$");

    public AdminCustomerView(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
        this.input = new ConsoleInput(new Scanner(System.in));
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== QUAN LY TAI KHOAN KHACH HANG =====\n" +
                    "1. Xem danh sach khach hang\n" +
                    "2. Tim kiem khach hang\n" +
                    "3. Them khach hang moi\n" +
                    "4. Cap nhat thong tin khach hang\n" +
                    "5. Xoa tai khoan khach hang\n" +
                    "6. Ban (Khoa) tai khoan\n" +
                    "7. Unban (Mo khoa) tai khoan\n" +
                    "0. Quay lai menu Admin");
            
            int choice = input.readInt("Nhap lua chon cua ban", 0);
            switch (choice) {
                case 1:
                    listCustomers();
                    break;
                case 2:
                    searchCustomer();
                    break;
                case 3:
                    addCustomer();
                    break;
                case 4:
                    updateCustomer();
                    break;
                case 5:
                    deleteCustomer();
                    break;
                case 6:
                    setCustomerStatus("BANNED");
                    break;
                case 7:
                    setCustomerStatus("ACTIVE");
                    break;
                case 0:
                    back = true;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
            }
        }
    }

    private void listCustomers() {
        System.out.println("\n--- DANH SACH KHACH HANG ---");
        List<Customer> all = customerRepo.findAll();
        if (all.isEmpty()) {
            System.out.println("Khong co khach hang nao.");
            return;
        }

        int limit = 20;
        int size = all.size();
        System.out.printf("Tong so khach hang: %d. Hien thi %d khach hang dau tien:%n", size, Math.min(limit, size));
        printTable(all.stream().limit(limit).collect(Collectors.toList()));
    }

    private void searchCustomer() {
        System.out.println("\n--- TIM KIEM KHACH HANG ---");
        String keyword = input.readLine("Nhap ID hoac Email can tim: ");
        if (keyword.isEmpty()) {
            System.out.println("Tu khoa khong duoc de trong.");
            return;
        }

        List<Customer> results = customerRepo.findAll().stream()
                .filter(c -> c.getCustomerId().equalsIgnoreCase(keyword) || c.getEmail().equalsIgnoreCase(keyword))
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            System.out.println("Khong tim thay khach hang phu hop.");
        } else {
            printTable(results);
        }
    }

    private void addCustomer() {
        System.out.println("\n--- THEM KHACH HANG MOI ---");
        String email = input.readLine("Email: ");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            System.out.println("Email khong dung dinh dang.");
            return;
        }

        Customer existing = customerRepo.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            System.out.println("Email nay da ton tai trong he thong.");
            return;
        }

        String name = input.readLine("Ho ten: ");
        if (name.isEmpty()) {
            System.out.println("Ten khong duoc de trong.");
            return;
        }

        String phone = input.readLine("So dien thoai (9-11 so): ");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            System.out.println("So dien thoai khong hop le.");
            return;
        }

        String address = input.readLine("Dia chi: ");
        if (address.isEmpty()) {
            System.out.println("Dia chi khong duoc de trong.");
            return;
        }

        String avatarUrl = input.readLine("Avatar URL (de trong neu khong co): ");
        CustTier tier = readTier();

        // Tu dong sinh ID dang C-XXXXX
        String newId = customerRepo.generateNewCustomerId();
        Customer customer = new Customer(newId, name, email, phone, address, avatarUrl, tier, "ACTIVE");
        customerRepo.save(customer);
        System.out.println("Them khach hang thanh cong! ID: " + newId);
    }

    private void updateCustomer() {
        System.out.println("\n--- CAP NHAT THONG TIN KHACH HANG ---");
        String id = input.readLine("Nhap ID khach hang can sua (VD: C-00001): ");
        Customer customer = customerRepo.findById(id);
        if (customer == null) {
            System.out.println("Khong tim thay khach hang.");
            return;
        }

        String name = input.readLine("Ho ten moi (De trong de giu nguyen: " + customer.getName() + "): ");
        if (!name.isEmpty()) customer.setName(name);

        String phone = input.readLine("So dien thoai moi (De trong de giu nguyen: " + customer.getPhone() + "): ");
        if (!phone.isEmpty()) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                System.out.println("So dien thoai khong hop le. Khong thay doi.");
            } else {
                customer.setPhone(phone);
            }
        }

        String address = input.readLine("Dia chi moi (De trong de giu nguyen: " + customer.getAddress() + "): ");
        if (!address.isEmpty()) customer.setAddress(address);

        String avatarUrl = input.readLine("Avatar URL moi (De trong de giu nguyen): ");
        if (!avatarUrl.isEmpty()) customer.setAvatarUrl(avatarUrl);

        System.out.println("Chon Hang thanh vien moi (Hien tai: " + customer.getTier() + "):");
        System.out.println("Nhan Enter neu khong muon thay doi.");
        String tierInput = input.readLine("Nhap Standard/Silver/Gold/Diamond: ");
        if (!tierInput.isEmpty()) {
            try {
                customer.setTier(CustTier.valueOf(tierInput.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Hang khong hop le. Khong thay doi.");
            }
        }

        customerRepo.save(customer);
        System.out.println("Cap nhat thong tin khach hang thanh cong!");
    }

    private void deleteCustomer() {
        System.out.println("\n--- XOA TAI KHOAN KHACH HANG ---");
        String id = input.readLine("Nhap ID khach hang can xoa: ");
        Customer customer = customerRepo.findById(id);
        if (customer == null) {
            System.out.println("Khong tim thay khach hang.");
            return;
        }

        String confirm = input.readLine("Ban co chac chan muon xoa khach hang " + customer.getName() + " (Y/N)?: ");
        if ("Y".equalsIgnoreCase(confirm)) {
            if (customerRepo.deleteById(id)) {
                System.out.println("Da xoa tai khoan thanh cong.");
            } else {
                System.out.println("Xoa that bai.");
            }
        } else {
            System.out.println("Huy thao tac xoa.");
        }
    }

    private void setCustomerStatus(String status) {
        System.out.println("\n--- " + ("BANNED".equals(status) ? "KHOA (BAN)" : "MO KHOA (UNBAN)") + " TAI KHOAN ---");
        Customer customer = customerRepo.findById(input.readLine("Nhap ID khach hang: "));
        if (customer == null) { System.out.println("Khong tim thay khach hang."); return; }
        if (status.equalsIgnoreCase(customer.getStatus())) { System.out.println("Tai khoan da o trang thai nay tu truoc."); return; }
        customer.setStatus(status);
        customerRepo.save(customer);
        System.out.println("Thanh cong! Trang thai cua " + customer.getName() + ": " + customer.getStatus());
    }

    private CustTier readTier() {
        while (true) {
            String val = input.readLine("Hang thanh vien (Standard/Silver/Gold/Diamond) [Standard]: ").trim().toUpperCase();
            if (val.isEmpty()) return CustTier.STANDARD;
            try { return CustTier.valueOf(val); } 
            catch (IllegalArgumentException e) { System.out.println("Hang khong hop le."); }
        }
    }

    private void printTable(List<Customer> list) {
        System.out.printf("%-10s | %-20s | %-30s | %-12s | %-10s | %-10s%n", 
                "ID", "Ho Ten", "Email", "Dien Thoai", "Hang", "Trang Thai");
        System.out.println("------------------------------------------------------------------------------------------------------");
        for (Customer c : list) {
            System.out.printf("%-10s | %-20s | %-30s | %-12s | %-10s | %-10s%n",
                    c.getCustomerId(),
                    c.getName().length() > 20 ? c.getName().substring(0, 17) + "..." : c.getName(),
                    c.getEmail().length() > 30 ? c.getEmail().substring(0, 27) + "..." : c.getEmail(),
                    c.getPhone(),
                    c.getTier(),
                    c.getStatus());
        }
    }
}
