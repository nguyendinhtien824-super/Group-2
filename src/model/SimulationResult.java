package model;

import java.util.Locale;

/**
 * Ket qua cua mot lan chay Simulation.
 */
public class SimulationResult {
    private String lockType;
    private int totalThreads;
    private int initialStock;
    private int successCount;
    private int successfulQuantity;
    private int failedCount;
    private int finalStock;
    private int negativeStock;
    private long durationMs;
    private double tps;             // Transactions per second
    private double vsBaselinePercent;
    private boolean dataConsistent;
    private int retryCount;         // Chi co voi Optimistic Lock
    private int runNumber = 1;

    public SimulationResult() {}

    // --- Getters & Setters ---
    public String getLockType() { return lockType; }
    public void setLockType(String lockType) { this.lockType = lockType; }

    public int getTotalThreads() { return totalThreads; }
    public void setTotalThreads(int totalThreads) { this.totalThreads = totalThreads; }

    public int getInitialStock() { return initialStock; }
    public void setInitialStock(int initialStock) { this.initialStock = initialStock; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getSuccessfulQuantity() { return successfulQuantity; }
    public void setSuccessfulQuantity(int successfulQuantity) {
        this.successfulQuantity = successfulQuantity;
    }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public int getFinalStock() { return finalStock; }
    public void setFinalStock(int finalStock) { this.finalStock = finalStock; }

    public int getNegativeStock() { return negativeStock; }
    public void setNegativeStock(int negativeStock) { this.negativeStock = negativeStock; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public double getTps() { return tps; }
    public void setTps(double tps) { this.tps = tps; }

    public double getVsBaselinePercent() { return vsBaselinePercent; }
    public void setVsBaselinePercent(double vsBaselinePercent) { this.vsBaselinePercent = vsBaselinePercent; }

    public boolean isDataConsistent() { return dataConsistent; }
    public void setDataConsistent(boolean dataConsistent) { this.dataConsistent = dataConsistent; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getRunNumber() { return runNumber; }
    public void setRunNumber(int runNumber) { this.runNumber = runNumber; }

    /**
     * Chuyen doi thanh dong CSV de ghi vao transactions.csv.
     */
    public String toCsvLine() {
        return String.join(",", lockType,
                String.valueOf(totalThreads), String.valueOf(initialStock),
                String.valueOf(successCount), String.valueOf(successfulQuantity),
                String.valueOf(failedCount),
                String.valueOf(finalStock), String.valueOf(negativeStock),
                String.valueOf(durationMs), String.format(Locale.ROOT, "%.2f", tps),
                String.valueOf(dataConsistent), String.valueOf(retryCount),
                String.valueOf(runNumber));
    }
}

