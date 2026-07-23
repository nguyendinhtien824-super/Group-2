package repository;

import model.Voucher;

/**
 * Repository xử lý I/O dữ liệu mã giảm giá (voucher) từ file CSV.
 */
public class VoucherRepository extends CsvRepository<Voucher> {
    public VoucherRepository() {
        super(Voucher.class, "vouchers.csv", "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
    }

    public VoucherRepository(String dataDirectory) {
        super(Voucher.class, dataDirectory, "vouchers.csv", "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
    }

    /**
     * Tìm kiếm mã giảm giá theo Code.
     */
    public Voucher findByCode(String code) {
        return findAll().stream()
                .filter(v -> v.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tự động sinh ID voucher mới dạng V-XXXXX dựa vào ID lớn nhất hiện tại.
     */
    public String generateNewVoucherId() {
        int maxId = findAll().stream()
                .map(Voucher::getVoucherId)
                .filter(id -> id != null && id.startsWith("V-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("V-%05d", maxId + 1);
    }
}
