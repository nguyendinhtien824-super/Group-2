package model;

import model.enums.CustTier;

public class Customer extends BaseEntity {
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private CustTier tier = CustTier.STANDARD;
    private String status = "ACTIVE";

    private String password;
    private double walletBalance = 1000000000.0;

    public Customer() {
        this.walletBalance = 1000000000.0;
    }

    public Customer(String customerId, String name, String email, String phone, String address, String avatarUrl) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.tier = CustTier.STANDARD;
        this.status = "ACTIVE";
        this.walletBalance = 1000000000.0;
    }

    public Customer(String customerId, String name, String email, String phone, String address, String avatarUrl, CustTier tier) {
        this(customerId, name, email, phone, address, avatarUrl);
        this.tier = tier != null ? tier : CustTier.STANDARD;
        this.status = "ACTIVE";
        this.walletBalance = 1000000000.0;
    }

    public Customer(String customerId, String name, String email, String phone, String address, String avatarUrl, CustTier tier, String status) {
        this(customerId, name, email, phone, address, avatarUrl, tier);
        this.status = status != null && !status.trim().isEmpty() ? status.trim() : "ACTIVE";
        this.walletBalance = 1000000000.0;
    }

    @Override
    public String getId() {
        return customerId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", customerId, name, email, phone, address, 
                avatarUrl != null ? avatarUrl : "", 
                tier != null ? tier.name() : CustTier.STANDARD.name(),
                status != null ? status : "ACTIVE",
                password != null ? password : "",
                String.format(java.util.Locale.US, "%.0f", walletBalance));
    }

    public static Customer fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        
        CustTier tierVal = CustTier.STANDARD;
        if (parts.length > 6 && !parts[6].trim().isEmpty()) {
            try {
                tierVal = CustTier.valueOf(parts[6].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                tierVal = CustTier.STANDARD;
            }
        }
        
        String statusVal = "ACTIVE";
        if (parts.length > 7 && !parts[7].trim().isEmpty()) {
            statusVal = parts[7].trim();
        }
        
        Customer c = new Customer(
            parts[0].trim(),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim(),
            parts.length > 5 ? parts[5].trim() : "",
            tierVal,
            statusVal
        );
        
        c.setPassword(parts.length > 8 && !parts[8].isEmpty() ? parts[8] : null);
        
        if (parts.length > 9 && !parts[9].trim().isEmpty()) {
            try {
                double parsedBalance = Double.parseDouble(parts[9].trim());
                c.setWalletBalance(Double.isFinite(parsedBalance) && parsedBalance >= 0
                        ? parsedBalance
                        : 1000000000.0);
            } catch (IllegalArgumentException e) {
                c.setWalletBalance(1000000000.0);
            }
        } else {
            c.setWalletBalance(1000000000.0);
        }
        return c;
    }

    // Getters and Setters
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) {
        if (!Double.isFinite(walletBalance) || walletBalance < 0) {
            throw new IllegalArgumentException("Wallet balance must be a finite non-negative number");
        }
        this.walletBalance = walletBalance;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public CustTier getTier() { return tier; }
    public void setTier(CustTier tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
