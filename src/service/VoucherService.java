package service;

import model.Voucher;
import repository.VoucherRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Validation, search and CRUD rules for vouchers. */
public class VoucherService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{3,30}$");
    private static final String PERCENTAGE = "PERCENTAGE";
    private static final String FIXED = "FIXED";

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = Objects.requireNonNull(voucherRepository, "voucherRepository");
    }

    public List<Voucher> list() {
        return voucherRepository.findAll().stream()
                .sorted((first, second) -> first.getCode().compareToIgnoreCase(second.getCode()))
                .toList();
    }

    public List<Voucher> listAvailable() {
        return list().stream()
                .filter(voucher -> voucher.getRemainingUses() > 0)
                .toList();
    }

    public List<Voucher> search(String keyword) {
        String normalized = required(keyword, "Từ khóa tìm kiếm").toLowerCase(Locale.ROOT);
        return voucherRepository.findAll().stream()
                .filter(voucher -> contains(voucher.getVoucherId(), normalized)
                        || contains(voucher.getCode(), normalized)
                        || contains(voucher.getType(), normalized))
                .toList();
    }

    public Voucher create(String code, String type, int value, int maxDiscount,
                          int minOrderAmount, int remainingUses) {
        VoucherData data = validate(code, type, value, maxDiscount,
                minOrderAmount, remainingUses);
        synchronized (voucherRepository) {
            ensureCodeAvailable(data.code(), null);
            Voucher voucher = new Voucher(voucherRepository.generateNewVoucherId(),
                    data.code(), data.type(), data.value(), data.maxDiscount(),
                    data.minOrderAmount(), data.remainingUses());
            voucherRepository.save(voucher);
            return voucher;
        }
    }

    public Voucher update(String idOrCode, String code, String type, Integer value,
                          Integer maxDiscount, Integer minOrderAmount, Integer remainingUses) {
        Voucher voucher = requireVoucher(idOrCode);
        String updatedCode = isBlank(code) ? voucher.getCode() : code;
        String updatedType = isBlank(type) ? voucher.getType() : type;
        int updatedValue = value == null ? voucher.getValue() : value;
        int updatedMax = maxDiscount == null ? voucher.getMaxDiscount() : maxDiscount;
        int updatedMin = minOrderAmount == null ? voucher.getMinOrderAmount() : minOrderAmount;
        int updatedUses = remainingUses == null ? voucher.getRemainingUses() : remainingUses;
        VoucherData data = validate(updatedCode, updatedType, updatedValue,
                updatedMax, updatedMin, updatedUses);

        synchronized (voucherRepository) {
            ensureCodeAvailable(data.code(), voucher.getVoucherId());
            voucher.setCode(data.code());
            voucher.setType(data.type());
            voucher.setValue(data.value());
            voucher.setMaxDiscount(data.maxDiscount());
            voucher.setMinOrderAmount(data.minOrderAmount());
            voucher.setRemainingUses(data.remainingUses());
            voucherRepository.save(voucher);
            return voucher;
        }
    }

    public Voucher delete(String idOrCode) {
        Voucher voucher = requireVoucher(idOrCode);
        if (!voucherRepository.deleteById(voucher.getVoucherId())) {
            throw new IllegalStateException("Không thể xóa voucher.");
        }
        return voucher;
    }

    private Voucher requireVoucher(String idOrCode) {
        String key = required(idOrCode, "Mã hoặc ID voucher");
        Voucher voucher = voucherRepository.findById(key);
        if (voucher == null) {
            voucher = voucherRepository.findByCode(key);
        }
        if (voucher == null) {
            throw new IllegalArgumentException("Không tìm thấy voucher: " + key);
        }
        return voucher;
    }

    private void ensureCodeAvailable(String code, String currentVoucherId) {
        boolean duplicate = voucherRepository.findAll().stream()
                .anyMatch(voucher -> voucher.getCode() != null
                        && voucher.getCode().equalsIgnoreCase(code)
                        && !voucher.getVoucherId().equalsIgnoreCase(
                                currentVoucherId == null ? "" : currentVoucherId));
        if (duplicate) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại.");
        }
    }

    private static VoucherData validate(String code, String type, int value,
                                        int maxDiscount, int minOrderAmount,
                                        int remainingUses) {
        String validCode = required(code, "Mã voucher").toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(validCode).matches()) {
            throw new IllegalArgumentException(
                    "Mã voucher phải gồm 3-30 ký tự chữ, số, gạch ngang hoặc gạch dưới.");
        }
        String validType = required(type, "Loại voucher").toUpperCase(Locale.ROOT);
        if (!PERCENTAGE.equals(validType) && !FIXED.equals(validType)) {
            throw new IllegalArgumentException("Loại voucher phải là PERCENTAGE hoặc FIXED.");
        }
        if (value <= 0 || PERCENTAGE.equals(validType) && value > 100) {
            throw new IllegalArgumentException(
                    "Giá trị giảm phải lớn hơn 0; voucher phần trăm không vượt quá 100.");
        }
        if (minOrderAmount < 0 || remainingUses < 0) {
            throw new IllegalArgumentException(
                    "Giá trị đơn tối thiểu và số lượt còn lại không được âm.");
        }
        int normalizedMax = FIXED.equals(validType) ? value : maxDiscount;
        if (PERCENTAGE.equals(validType) && normalizedMax <= 0) {
            throw new IllegalArgumentException("Mức giảm tối đa phải lớn hơn 0.");
        }
        return new VoucherData(validCode, validType, value, normalizedMax,
                minOrderAmount, remainingUses);
    }

    private static String required(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " không được để trống.");
        }
        return value.trim();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record VoucherData(String code, String type, int value, int maxDiscount,
                               int minOrderAmount, int remainingUses) {
    }
}
