package repository;

import model.FlashSaleEvent;

public class FlashSaleEventRepository extends CsvRepository<FlashSaleEvent> {
    public FlashSaleEventRepository() {
        super("flash_events.csv", "eventId,name,startTime,endTime,status");
    }

    @Override
    protected FlashSaleEvent parseLine(String line) {
        return FlashSaleEvent.fromCsvLine(line);
    }
}
