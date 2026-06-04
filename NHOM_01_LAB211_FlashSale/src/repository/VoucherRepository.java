package repository;

import model.Voucher;

public class VoucherRepository extends CsvRepository<Voucher> {
    public VoucherRepository() {
        super("vouchers.csv", "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
    }

    public VoucherRepository(String dataDirectory) {
        super(dataDirectory, "vouchers.csv", "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
    }

    @Override
    protected Voucher parseLine(String line) {
        return Voucher.fromCsvLine(line);
    }
}
