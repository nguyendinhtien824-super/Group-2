package controller;

import model.FlashItem;
import repository.FlashItemRepository;

import java.util.List;
import java.util.stream.Collectors;

public class FlashSaleController {

    private final FlashItemRepository flashItemRepository;

    public FlashSaleController(FlashItemRepository flashItemRepository) {
        this.flashItemRepository = flashItemRepository;
    }

    public List<FlashItem> getFlashSaleItems(int limit) {
        return flashItemRepository.findAll().stream()
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }
}

