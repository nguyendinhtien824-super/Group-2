package model;

public class Voucher extends BaseEntity {
    private String voucherId;
    private String code;
    private String type; // PERCENTAGE, FIXED
    private int value; // 10 (10%) or 30000 (30,000 VND)
    private int maxDiscount; // Max discount for PERCENTAGE, e.g. 50000
    private int minOrderAmount; // Min order subtotal required to apply
    private int remainingUses; // Number of remaining uses

    public Voucher() {}

    public Voucher(String voucherId, String code, String type, int value, int maxDiscount, int minOrderAmount, int remainingUses) {
        this.voucherId = voucherId;
        this.code = code;
        this.type = type;
        this.value = value;
        this.maxDiscount = maxDiscount;
        this.minOrderAmount = minOrderAmount;
        this.remainingUses = remainingUses;
    }

    @Override
    public String getId() {
        return voucherId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", 
                voucherId, 
                code, 
                type, 
                String.valueOf(value), 
                String.valueOf(maxDiscount), 
                String.valueOf(minOrderAmount), 
                String.valueOf(remainingUses));
    }

    public static Voucher fromCsvLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 7) return null;
        try {
            return new Voucher(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    Integer.parseInt(parts[3].trim()),
                    Integer.parseInt(parts[4].trim()),
                    Integer.parseInt(parts[5].trim()),
                    Integer.parseInt(parts[6].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Getters and Setters
    public String getVoucherId() { return voucherId; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public int getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(int maxDiscount) { this.maxDiscount = maxDiscount; }

    public int getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(int minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public int getRemainingUses() { return remainingUses; }
    public void setRemainingUses(int remainingUses) { this.remainingUses = remainingUses; }
}
