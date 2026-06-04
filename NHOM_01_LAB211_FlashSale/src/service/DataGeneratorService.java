package service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DataGeneratorService - Tao du lieu CSV gia lap.
 *
 * Tao du 10,000+ dong tong cong theo yeu cau PDF:
 *   - products.csv     >= 5,000 dong
 *   - customers.csv    >= 2,000 dong
 *   - flash_events.csv >= vÃ i dong
 *   - flash_items.csv  >= 500 dong
 *   - orders.csv       >= 2,500 dong
 *   - order_details.csv >= 2,500 dong
 */
public class DataGeneratorService {

    private final String dataDirectory;

    public DataGeneratorService() {
        this("data");
    }

    public DataGeneratorService(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    private static final String[] PRODUCT_PREFIXES = {
            "Tai Nghe", "Ao Thun", "Chuot Gaming", "Ban Phim", "Loa",
            "Dong Ho", "Giay", "Balo", "Sac Du Phong", "Kinh Mat",
            "Quat Mini", "Webcam", "USB Hub", "Cap Sac", "Op Lung",
            "Ong Nhom", "May Anh", "Tablet", "Laptop", "Monitor"
    };

    private static final String[] PRODUCT_SUFFIXES = {
            "Pro Max", "Ultra", "Lite", "Plus", "Premium",
            "V2", "Classic", "Sport", "Mini", "XL"
    };

    private static final String[] BRANDS = {
            "Samsung", "Xiaomi", "Apple", "Sony", "JBL",
            "Logitech", "Razer", "Asus", "HP", "Anker"
    };

    private static final String[] FIRST_NAMES = {
            "Nguyen", "Tran", "Le", "Pham", "Hoang",
            "Phan", "Vu", "Vo", "Dang", "Bui"
    };

    private static final String[] LAST_NAMES = {
            "Anh", "Binh", "Cuong", "Dung", "Hieu",
            "Hung", "Khanh", "Linh", "Minh", "Nam",
            "Phuc", "Quang", "Son", "Thanh", "Trung"
    };

    /**
     * Tao tat ca file CSV.
     * @return So dong da tao
     */
    public Map<String, Integer> generateAll() throws IOException {
        Files.createDirectories(Paths.get(dataDirectory));

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("products.csv", generateProducts(5000));
        counts.put("customers.csv", generateCustomers(2000));
        counts.put("vouchers.csv", generateVouchers(10));
        counts.put("flash_events.csv", generateFlashEvents(10));
        counts.put("flash_items.csv", generateFlashItems(500));
        counts.put("orders.csv", generateOrders(2500));
        counts.put("order_details.csv", generateOrderDetails(2500));
        counts.put("transactions.csv", resetTransactions());

        int total = counts.entrySet().stream()
                .filter(entry -> !"transactions.csv".equals(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        counts.put("TOTAL", total);
        return counts;
    }

    private int generateProducts(int count) throws IOException {
        try (BufferedWriter w = newWriter("products.csv")) {
            w.write("productId,name,brand,category,price,stock,description");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String id = String.format("P-%05d", i);
                String prefix = pick(PRODUCT_PREFIXES);
                String suffix = pick(PRODUCT_SUFFIXES);
                String brand = pick(BRANDS);
                int price = randomInt(50, 5000) * 1000;
                int stock = randomInt(10, 500);
                w.write(String.format("%s,%s %s %s,%s,%s,%d,%d,San pham chat luong cao",
                        id, brand, prefix, suffix, brand, prefix, price, stock));
                w.newLine();
            }
        }
        return count;
    }

    private int generateCustomers(int count) throws IOException {
        try (BufferedWriter w = newWriter("customers.csv")) {
            w.write("customerId,name,email,phone,address,avatarUrl,tier,status");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String id = String.format("C-%05d", i);
                String first = pick(FIRST_NAMES);
                String last = pick(LAST_NAMES);
                String name = first + " Van " + last;
                String email = (first + last + i + "@gmail.com").toLowerCase();
                String phone = "09" + randomInt(10000000, 99999999);
                
                double rand = Math.random();
                String tier = "STANDARD";
                if (rand > 0.95) tier = "DIAMOND";
                else if (rand > 0.80) tier = "GOLD";
                else if (rand > 0.60) tier = "SILVER";

                w.write(String.format("%s,%s,%s,%s,Ha Noi,,%s,ACTIVE", id, name, email, phone, tier));
                w.newLine();
            }
        }
        return count;
    }

    private int generateVouchers(int count) throws IOException {
        try (BufferedWriter w = newWriter("vouchers.csv")) {
            w.write("voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
            w.newLine();
            
            w.write("V-00001,SHOPEE10,PERCENTAGE,10,50000,0,1000"); w.newLine();
            w.write("V-00002,FREESHIP,FIXED,30000,30000,100000,1000"); w.newLine();
            w.write("V-00003,GOLDVIP,FIXED,50000,50000,150000,500"); w.newLine();
            w.write("V-00004,DIAMONDSALE,PERCENTAGE,20,100000,200000,500"); w.newLine();
            w.write("V-00005,CHAOHE,PERCENTAGE,15,30000,50000,2000"); w.newLine();
            
            for (int i = 6; i <= count; i++) {
                String id = String.format("V-%05d", i);
                String code = "SALE" + randomInt(10, 50);
                String type = Math.random() > 0.5 ? "PERCENTAGE" : "FIXED";
                int value = type.equals("PERCENTAGE") ? randomInt(5, 25) : randomInt(10, 50) * 1000;
                int maxDiscount = type.equals("PERCENTAGE") ? randomInt(20, 100) * 1000 : value;
                int minOrder = randomInt(0, 150) * 1000;
                int uses = randomInt(50, 500);
                w.write(String.format("%s,%s,%s,%d,%d,%d,%d", id, code, type, value, maxDiscount, minOrder, uses));
                w.newLine();
            }
        }
        return count;
    }

    private int generateFlashEvents(int count) throws IOException {
        try (BufferedWriter w = newWriter("flash_events.csv")) {
            w.write("eventId,name,startTime,endTime,status");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String id = String.format("EV-%03d", i);
                int hour = 9 + (i % 12);
                w.write(String.format("%s,Flash Sale %02d:00,2025-06-%02d %02d:00:00,2025-06-%02d %02d:00:00,ACTIVE",
                        id, hour, i, hour, i, hour + 2));
                w.newLine();
            }
        }
        return count;
    }

    private int generateFlashItems(int count) throws IOException {
        try (BufferedWriter w = newWriter("flash_items.csv")) {
            w.write("itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,version");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String itemId = String.format("FI-%05d", i);
                String productId = String.format("P-%05d", randomInt(1, 5000));
                String eventId = String.format("EV-%03d", randomInt(1, 10));
                String name = pick(BRANDS) + " " + pick(PRODUCT_PREFIXES) + " " + pick(PRODUCT_SUFFIXES);
                int originalPrice = randomInt(100, 3000) * 1000;
                int salePrice = (int) (originalPrice * (0.3 + Math.random() * 0.4));
                int stock = randomInt(5, 200);
                w.write(String.format("%s,%s,%s,%s,%d,%d,%d,0,0",
                        itemId, productId, eventId, name, originalPrice, salePrice, stock));
                w.newLine();
            }
        }
        return count;
    }

    private int generateOrders(int count) throws IOException {
        try (BufferedWriter w = newWriter("orders.csv")) {
            w.write("orderId,customerId,orderDate,totalAmount,status");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String orderId = String.format("O-%06d", i);
                String customerId = String.format("C-%05d", randomInt(1, 2000));
                String date = String.format("2025-06-%02d", randomInt(1, 30));
                int total = randomInt(100, 5000) * 1000;
                String status = Math.random() > 0.1 ? "SUCCESS" : "FAILED";
                w.write(String.format("%s,%s,%s,%d,%s", orderId, customerId, date, total, status));
                w.newLine();
            }
        }
        return count;
    }

    private int generateOrderDetails(int count) throws IOException {
        try (BufferedWriter w = newWriter("order_details.csv")) {
            w.write("detailId,orderId,productId,quantity,unitPrice,subtotal");
            w.newLine();
            for (int i = 1; i <= count; i++) {
                String detailId = String.format("D-%06d", i);
                String orderId = String.format("O-%06d", randomInt(1, 2500));
                String productId = String.format("P-%05d", randomInt(1, 5000));
                int qty = randomInt(1, 5);
                int unitPrice = randomInt(50, 3000) * 1000;
                w.write(String.format("%s,%s,%s,%d,%d,%d",
                        detailId, orderId, productId, qty, unitPrice, qty * unitPrice));
                w.newLine();
            }
        }
        return count;
    }

    private int resetTransactions() throws IOException {
        try (BufferedWriter w = newWriter("transactions.csv")) {
            w.write("transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
            w.newLine();
        }
        return 0;
    }

    private BufferedWriter newWriter(String fileName) throws IOException {
        Path path = Paths.get(dataDirectory, fileName);
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }

    private String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)];
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

