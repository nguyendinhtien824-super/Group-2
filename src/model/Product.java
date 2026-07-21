package model;

public class Product extends BaseEntity {
    private String productId;
    private String name;
    private String brand;
    private String category;
    private int price;
    private int stock;
    private String description;
    private int version;

    public Product() {}

    public Product(String productId, String name, String brand, String category, int price, int stock, String description) {
        this(productId, name, brand, category, price, stock, description, 0);
    }

    public Product(String productId, String name, String brand, String category,
                   int price, int stock, String description, int version) {
        this.productId = productId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.version = version;
    }

    @Override
    public String getId() {
        return productId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", productId, name, brand, category, String.valueOf(price),
                String.valueOf(stock), description, String.valueOf(version));
    }

    public static Product fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 7) return null;
        return new Product(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            Integer.parseInt(parts[4].trim()),
            Integer.parseInt(parts[5].trim()),
            parts[6].trim(),
            parts.length >= 8 ? Integer.parseInt(parts[7].trim()) : 0
        );
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getVersion() { return version; }
    public void setVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("Phiên bản sản phẩm không được âm");
        }
        this.version = version;
    }
}

