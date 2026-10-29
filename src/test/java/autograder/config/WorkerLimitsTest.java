package autograder.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerLimitsTest {
    @Test
    void clampsProcessorsToOneThroughThirtyTwo() {
        assertEquals(1, new WorkerLimits(() -> 0).processors());
        assertEquals(1, new WorkerLimits(() -> -3).processors());
        assertEquals(8, new WorkerLimits(() -> 8).processors());
        assertEquals(32, new WorkerLimits(() -> 64).processors());
    }

    @Test
    void autoWorkersIsProcessorCountUpToFour() {
        assertEquals(1, new WorkerLimits(() -> 1).autoWorkers());
        assertEquals(2, new WorkerLimits(() -> 2).autoWorkers());
        assertEquals(4, new WorkerLimits(() -> 4).autoWorkers());
        assertEquals(4, new WorkerLimits(() -> 16).autoWorkers());
    }

    @Test
    void capLeavesRequestsWithinProcessorCountUnchanged() {
        WorkerLimits limits = new WorkerLimits(() -> 4);
        WorkerLimits.Capped below = limits.cap(2);
        assertEquals(2, below.workers());
        assertTrue(below.warning().isEmpty());
        WorkerLimits.Capped equal = limits.cap(4);
        assertEquals(4, equal.workers());
        assertTrue(equal.warning().isEmpty());
    }

    @Test
    void capReducesExcessRequestsWithWarning() {
        WorkerLimits.Capped capped = new WorkerLimits(() -> 4).cap(8);
        assertEquals(4, capped.workers());
        assertEquals("execution.workers=8 exceeds 4 available processors; using 4",
                capped.warning().orElseThrow());
    }

    @Test
    void systemLimitsReportAtLeastOneProcessor() {
        assertTrue(WorkerLimits.system().processors() >= 1);
    }
}
