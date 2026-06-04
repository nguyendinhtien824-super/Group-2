package model;

/**
 * Flash Sale Item - san pham tham gia flash sale.
 * Chua truong version cho Optimistic Locking.
 */
public class FlashItem extends BaseEntity {
    private String itemId;
    private String productId;
    private String eventId;
    private String productName;
    private int originalPrice;
    private int salePrice;
    private int initialStock;    // Ton kho ban dau
    private int soldQty;         // So luong da ban
    private int version;         // Version cho Optimistic Lock

    public FlashItem() {}

    public FlashItem(String itemId, String productId, String eventId,
                     String productName, int originalPrice, int salePrice,
                     int initialStock) {
        this.itemId = itemId;
        this.productId = productId;
        this.eventId = eventId;
        this.productName = productName;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.initialStock = initialStock;
        this.soldQty = 0;
        this.version = 0;
    }

    /**
     * Tinh so luong con lai.
     * Co the am neu dung NO_LOCK (race condition).
     */
    public int getRemainingStock() {
        return initialStock - soldQty;
    }

    // --- Getters & Setters ---
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(int originalPrice) { this.originalPrice = originalPrice; }

    public int getSalePrice() { return salePrice; }
    public void setSalePrice(int salePrice) { this.salePrice = salePrice; }

    public int getInitialStock() { return initialStock; }
    public void setInitialStock(int initialStock) { this.initialStock = initialStock; }

    public int getSoldQty() { return soldQty; }
    public void setSoldQty(int soldQty) { this.soldQty = soldQty; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    @Override
    public String getId() {
        return itemId;
    }

    /**
     * Chuyen doi thanh dong CSV.
     */
    @Override
    public String toCsvLine() {
        return String.join(",", itemId, productId, eventId, productName,
                String.valueOf(originalPrice), String.valueOf(salePrice),
                String.valueOf(initialStock), String.valueOf(soldQty),
                String.valueOf(version));
    }

    /**
     * Parse tu dong CSV.
     */
    public static FlashItem fromCsvLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 9) return null;

        FlashItem item = new FlashItem();
        item.setItemId(parts[0].trim());
        item.setProductId(parts[1].trim());
        item.setEventId(parts[2].trim());
        item.setProductName(parts[3].trim());
        item.setOriginalPrice(Integer.parseInt(parts[4].trim()));
        item.setSalePrice(Integer.parseInt(parts[5].trim()));
        item.setInitialStock(Integer.parseInt(parts[6].trim()));
        item.setSoldQty(Integer.parseInt(parts[7].trim()));
        item.setVersion(Integer.parseInt(parts[8].trim()));
        return item;
    }
}

