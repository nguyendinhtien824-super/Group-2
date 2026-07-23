package test;

import model.SimulationResult;
import org.junit.Test;
import service.SimulatorPerformanceTarget;

import static org.junit.Assert.assertEquals;

public class SimulatorPerformanceTargetTest {
    @Test
    public void noLockIsAlwaysReportedAsBaseline() {
        SimulationResult result = result("NO_LOCK", 100, false, -99.0);

        assertEquals(SimulatorPerformanceTarget.Verdict.BASELINE,
                SimulatorPerformanceTarget.evaluate(result));
    }

    @Test
    public void safeResultAtThirtyPercentDropMeetsTarget() {
        SimulationResult result = result("SYNCHRONIZED", 0, true, -30.0);

        assertEquals(SimulatorPerformanceTarget.Verdict.DAT,
                SimulatorPerformanceTarget.evaluate(result));
    }

    @Test
    public void unsafeOrSlowerResultDoesNotMeetTarget() {
        SimulationResult unsafe = result("FILE_LOCK", 1, false, -10.0);
        SimulationResult slower = result("OPTIMISTIC_LOCK", 0, true, -30.1);

        assertEquals(SimulatorPerformanceTarget.Verdict.CHUA_DAT,
                SimulatorPerformanceTarget.evaluate(unsafe));
        assertEquals(SimulatorPerformanceTarget.Verdict.CHUA_DAT,
                SimulatorPerformanceTarget.evaluate(slower));
    }

    private SimulationResult result(String lockType, int negativeStock,
                                    boolean consistent, double versusBaseline) {
        SimulationResult result = new SimulationResult();
        result.setLockType(lockType);
        result.setNegativeStock(negativeStock);
        result.setDataConsistent(consistent);
        result.setVsBaselinePercent(versusBaseline);
        return result;
    }
}
