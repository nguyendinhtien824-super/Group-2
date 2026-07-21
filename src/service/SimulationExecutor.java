package service;

import model.OrderTransaction;
import model.enums.CustTier;
import model.enums.LockType;
import repository.FlashItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Executes one simultaneous-start simulation against a real CSV repository. */
final class SimulationExecutor {
    private static final int READY_TIMEOUT_SECONDS = 60;
    private static final int RUN_TIMEOUT_SECONDS = 120;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    Outcome execute(
            FlashItemRepository repository,
            LockType lockType,
            int threadCount,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        AtomicInteger successfulQuantity = new AtomicInteger();
        AtomicInteger retryCount = new AtomicInteger();
        ConcurrentLinkedQueue<OrderTransaction> transactions = new ConcurrentLinkedQueue<>();
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        boolean completed = false;

        try {
            for (int buyerNumber = 1; buyerNumber <= threadCount; buyerNumber++) {
                int currentBuyer = buyerNumber;
                executor.execute(() -> executeBuyer(
                        repository, lockType, maxRetries, tierComposition, currentBuyer,
                        readyLatch, startLatch, doneLatch, successCount, failedCount,
                        successfulQuantity, retryCount, transactions));
            }

            if (!readyLatch.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers did not become ready in time");
            }
            long startNanos = System.nanoTime();
            startLatch.countDown();
            if (!doneLatch.await(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Simulation did not finish in time");
            }
            long durationNanos = Math.max(1L, System.nanoTime() - startNanos);
            completed = true;
            return new Outcome(
                    successCount.get(), failedCount.get(), successfulQuantity.get(),
                    retryCount.get(), durationNanos, new ArrayList<>(transactions));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation was interrupted", exception);
        } finally {
            startLatch.countDown();
            shutdownExecutor(executor, completed);
        }
    }

    private void executeBuyer(
            FlashItemRepository repository,
            LockType lockType,
            int maxRetries,
            Map<CustTier, Double> tierComposition,
            int buyerNumber,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            CountDownLatch doneLatch,
            AtomicInteger successCount,
            AtomicInteger failedCount,
            AtomicInteger successfulQuantity,
            AtomicInteger retryCount,
            ConcurrentLinkedQueue<OrderTransaction> transactions) {
        CustTier tier = CustTier.STANDARD;
        int quantity = 1;
        boolean success = false;
        String detail = "Request interrupted";
        readyLatch.countDown();
        try {
            startLatch.await();
            tier = selectTier(tierComposition);
            quantity = selectQuantity(tier);
            boolean appliedVoucher = ThreadLocalRandom.current().nextDouble() < 0.25;
            success = purchase(repository, lockType, quantity, maxRetries, retryCount);
            detail = String.format("Tier: %s | Qty: %d | Voucher: %b",
                    tier.name(), quantity, appliedVoucher);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException exception) {
            detail = "Storage operation failed";
        } finally {
            if (success) {
                successCount.incrementAndGet();
                successfulQuantity.addAndGet(quantity);
            } else {
                failedCount.incrementAndGet();
            }
            transactions.add(createTransaction(
                    buyerNumber, quantity, success, detail));
            doneLatch.countDown();
        }
    }

    private boolean purchase(
            FlashItemRepository repository,
            LockType lockType,
            int quantity,
            int maxRetries,
            AtomicInteger retryCount) {
        return switch (lockType) {
            case NO_LOCK -> repository.sellNoLock(SimulatorService.ITEM_ID, quantity);
            case FILE_LOCK -> repository.sellWithFileLock(SimulatorService.ITEM_ID, quantity);
            case SYNCHRONIZED -> repository.sellWithSynchronized(SimulatorService.ITEM_ID, quantity);
            case OPTIMISTIC_LOCK -> repository.sellWithOptimisticLock(
                    SimulatorService.ITEM_ID, quantity, maxRetries, retryCount);
        };
    }

    private CustTier selectTier(Map<CustTier, Double> composition) {
        double randomValue = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (Map.Entry<CustTier, Double> entry : composition.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue < cumulative) {
                return entry.getKey();
            }
        }
        return CustTier.STANDARD;
    }

    private int selectQuantity(CustTier tier) {
        return tier == CustTier.STANDARD
                ? 1
                : ThreadLocalRandom.current().nextInt(1, 3);
    }

    private OrderTransaction createTransaction(
            int buyerNumber,
            int quantity,
            boolean success,
            String detail) {
        return new OrderTransaction(
                UUID.randomUUID().toString(),
                "ORD-" + UUID.randomUUID(),
                "CUST-" + buyerNumber,
                SimulatorService.ITEM_ID,
                quantity,
                success ? "SUCCESS" : "FAILED",
                (success ? "Processed: " : "Rejected: ") + detail,
                System.currentTimeMillis());
    }

    private void shutdownExecutor(ExecutorService executor, boolean completed) {
        if (completed) {
            executor.shutdown();
        } else {
            executor.shutdownNow();
        }
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    record Outcome(
            int successCount,
            int failedCount,
            int successfulQuantity,
            int retryCount,
            long durationNanos,
            List<OrderTransaction> transactions) {
    }
}
