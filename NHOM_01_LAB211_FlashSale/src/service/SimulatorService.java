package service;

import model.FlashItem;
import model.SimulationResult;
import model.enums.LockType;
import model.enums.CustTier;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import repository.OrderTransactionRepository;
import model.OrderTransaction;

/**
 * Su dung CountDownLatch de dam bao tat ca threads
 * bat dau dong thoi (PDF yeu cau bat buoc).
 */
public class SimulatorService {

    private final OrderTransactionRepository transactionRepo;

    public SimulatorService(OrderTransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    public SimulationResult runSimulation(LockType lockType, int numThreads, int stock) {
        java.util.Map<CustTier, Double> defaultComposition = java.util.Map.of(
            CustTier.STANDARD, 0.60,
            CustTier.SILVER, 0.20,
            CustTier.GOLD, 0.15,
            CustTier.DIAMOND, 0.05
        );
        return runSimulation(lockType, numThreads, stock, 100, defaultComposition);
    }

    /**
     * Chay simulation voi cấu hình nâng cao.
     */
    public SimulationResult runSimulation(LockType lockType, int numThreads, int stock, int maxRetries, java.util.Map<CustTier, Double> tierComposition) {
        // Reset FlashItem cho moi lan chay
        FlashItem item = new FlashItem(
                "FI-001", "P-001", "EV-001",
                "Flash Sale Product", 500000, 199000, stock
        );

        SimulationResult result = new SimulationResult();
        result.setLockType(lockType.name());
        result.setTotalThreads(numThreads);
        result.setInitialStock(stock);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        
        ConcurrentLinkedQueue<OrderTransaction> txQueue = new ConcurrentLinkedQueue<>();

        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        int poolSize = Math.min(numThreads, 4000);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        ReentrantLock pessimisticLock = new ReentrantLock();
        long startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // Chọn thứ hạng thành viên dựa trên phân bổ
                    CustTier tier = selectTier(tierComposition);
                    int quantity = 1;
                    switch (tier) {
                        case DIAMOND:
                            quantity = ThreadLocalRandom.current().nextInt(1, 4); // 1-3
                            break;
                        case GOLD:
                        case SILVER:
                            quantity = ThreadLocalRandom.current().nextInt(1, 3); // 1-2
                            break;
                        case STANDARD:
                        default:
                            quantity = 1;
                            break;
                    }

                    // Tăng số lần thử lại đối với khách hàng VIP (Priority checkout)
                    int threadMaxRetries = maxRetries;
                    if (tier == CustTier.DIAMOND || tier == CustTier.GOLD) {
                        threadMaxRetries = (int) (maxRetries * 1.5);
                    }

                    // Giả lập áp dụng voucher ngẫu nhiên cho 25% các thread
                    boolean appliedVoucher = ThreadLocalRandom.current().nextDouble() < 0.25;

                    boolean success = false;
                    switch (lockType) {
                        case NO_LOCK:
                            success = purchaseNoLock(item, quantity);
                            break;
                        case FILE_LOCK:
                            success = purchaseFileLock(item, pessimisticLock, quantity);
                            break;
                        case SYNCHRONIZED:
                            success = purchaseSynchronized(item, quantity);
                            break;
                        case OPTIMISTIC_LOCK:
                            success = purchaseOptimistic(item, retryCount, quantity, threadMaxRetries);
                            break;
                    }

                    String msg = String.format("Tier: %s | Qty: %d | Voucher: %b", tier.name(), quantity, appliedVoucher);

                    if (success) {
                        successCount.incrementAndGet();
                        txQueue.add(new OrderTransaction(
                            UUID.randomUUID().toString(),
                            "ORD-" + System.nanoTime(),
                            "CUST-" + Thread.currentThread().getId(),
                            item.getId(),
                            quantity,
                            "SUCCESS",
                            "Processed: " + msg,
                            System.currentTimeMillis()
                        ));
                    } else {
                        failedCount.incrementAndGet();
                        txQueue.add(new OrderTransaction(
                            UUID.randomUUID().toString(),
                            "ORD-" + System.nanoTime(),
                            "CUST-" + Thread.currentThread().getId(),
                            item.getId(),
                            quantity,
                            "FAILED",
                            "Depleted: " + msg,
                            System.currentTimeMillis()
                        ));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        try {
            readyLatch.await(15, TimeUnit.SECONDS);
            startLatch.countDown();
            doneLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        result.setSuccessCount(successCount.get());
        result.setFailedCount(failedCount.get());
        result.setFinalStock(item.getRemainingStock());
        result.setNegativeStock(Math.min(0, item.getRemainingStock()));
        result.setDurationMs(durationMs);
        result.setRetryCount(retryCount.get());

        double durationSec = Math.max(durationMs / 1000.0, 0.001);
        int totalProcessed = successCount.get() + failedCount.get();
        result.setTps(Math.round(totalProcessed / durationSec));
        result.setDataConsistent(item.getRemainingStock() >= 0);

        transactionRepo.saveAll(new java.util.ArrayList<>(txQueue));

        return result;
    }

    private CustTier selectTier(java.util.Map<CustTier, Double> composition) {
        double rand = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (java.util.Map.Entry<CustTier, Double> entry : composition.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }
        return CustTier.STANDARD;
    }

    private boolean purchaseNoLock(FlashItem item, int quantity) {
        int currentSold = item.getSoldQty();
        int remaining = item.getInitialStock() - currentSold;

        if (remaining >= quantity) {
            simulateProcessing();
            item.setSoldQty(item.getSoldQty() + quantity);
            return true;
        }
        return false;
    }

    private boolean purchaseFileLock(FlashItem item, ReentrantLock lock, int quantity) {
        lock.lock();
        try {
            if (item.getRemainingStock() >= quantity) {
                item.setSoldQty(item.getSoldQty() + quantity);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private boolean purchaseSynchronized(FlashItem item, int quantity) {
        synchronized (item) {
            if (item.getRemainingStock() >= quantity) {
                item.setSoldQty(item.getSoldQty() + quantity);
                return true;
            }
            return false;
        }
    }

    private boolean purchaseOptimistic(FlashItem item, AtomicInteger retryCounter, int quantity, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            int currentVersion = item.getVersion();
            int currentSold = item.getSoldQty();
            int remaining = item.getInitialStock() - currentSold;

            if (remaining < quantity) {
                return false; 
            }

            synchronized (item) {
                if (item.getVersion() == currentVersion) {
                    item.setSoldQty(currentSold + quantity);
                    item.setVersion(currentVersion + 1);
                    return true;
                }
            }
            retryCounter.incrementAndGet();
        }
        return false; 
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(0, 500_000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

