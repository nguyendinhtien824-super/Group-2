package service;

import model.SimulationResult;
import model.enums.LockType;

import java.util.Objects;

/** Pure evaluator for the PDF safety and throughput target. */
public final class SimulatorPerformanceTarget {
    public static final double MAX_THROUGHPUT_DROP_PERCENT = 30.0;

    private SimulatorPerformanceTarget() {
    }

    public static Verdict evaluate(SimulationResult result) {
        Objects.requireNonNull(result, "result");
        if (result.getLockType() != null
                && result.getLockType().startsWith(LockType.NO_LOCK.name())) {
            return Verdict.BASELINE;
        }
        boolean safe = result.getNegativeStock() == 0 && result.isDataConsistent();
        boolean throughputAccepted = Double.isFinite(result.getVsBaselinePercent())
                && result.getVsBaselinePercent() >= -MAX_THROUGHPUT_DROP_PERCENT;
        return safe && throughputAccepted ? Verdict.DAT : Verdict.CHUA_DAT;
    }

    public enum Verdict {
        BASELINE,
        DAT,
        CHUA_DAT
    }
}
