package view;

import model.FlashItem;

import java.util.List;

public class FlashSaleView {
    public void displayItems(List<FlashItem> items) {
        if (items.isEmpty()) {
            System.out.println("Chưa có sản phẩm Flash Sale. Hãy tạo dữ liệu CSV trước.");
            return;
        }

        System.out.printf("%-15s %-32s %12s %12s %10s%n",
                "Mã SP (ItemId)", "Tên sản phẩm", "Giá gốc", "Giá sale", "Còn lại");
        System.out.println("-------------------------------------------------------------------------------");
        for (FlashItem item : items) {
            System.out.printf("%-15s %-32s %,12d %,12d %10d%n",
                    item.getItemId(),
                    trim(item.getProductName(), 32),
                    item.getOriginalPrice(),
                    item.getSalePrice(),
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
