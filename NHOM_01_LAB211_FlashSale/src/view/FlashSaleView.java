package view;

import model.FlashItem;

import java.util.List;

public class FlashSaleView {
    public void displayItems(List<FlashItem> items) {
        if (items.isEmpty()) {
            System.out.println("Chua co san pham Flash Sale. Hay tao du lieu CSV truoc.");
            return;
        }

        System.out.printf("%-10s %-32s %12s %12s %10s %10s%n",
                "ItemId", "Ten san pham", "Gia goc", "Gia sale", "Da ban", "Con lai");
        System.out.println("----------------------------------------------------------------------------------------");
        for (FlashItem item : items) {
            System.out.printf("%-10s %-32s %,12d %,12d %10d %10d%n",
                    item.getItemId(),
                    trim(item.getProductName(), 32),
                    item.getOriginalPrice(),
                    item.getSalePrice(),
                    item.getSoldQty(),
                    item.getRemainingStock());
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
