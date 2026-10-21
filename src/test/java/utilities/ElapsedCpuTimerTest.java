package utilities;

import org.junit.Test;

import static org.junit.Assert.*;

public class ElapsedCpuTimerTest {

    // Busy-waits rather than sleeping: on Linux the timers measure thread CPU time, which a sleep does not use
    public static void spin(long millis) {
        long end = System.nanoTime() + millis * 1_000_000;
        while (System.nanoTime() < end) {
            // burn CPU
        }
    }

    @Test
    public void elapsedTracksBusyTime() {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        spin(50);
        long elapsed = timer.elapsedMillis();
        assertTrue("elapsed " + elapsed, elapsed >= 40 && elapsed < 1000);
    }

    @Test
    public void resetRestartsTheTimer() {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        spin(50);
        timer.reset();
        assertTrue(timer.elapsedMillis() < 20);
    }

    @Test
    public void exceededMaxTimeOnceBudgetIsSpent() {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        timer.setMaxTimeMillis(30);
        assertFalse(timer.exceededMaxTime());
        assertTrue(timer.remainingTimeMillis() <= 30);
        spin(50);
        assertTrue(timer.exceededMaxTime());
        assertTrue(timer.remainingTimeMillis() < 0);
    }

    @Test
    public void enoughBudgetIterationUsesAverageIterationTime() {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        timer.setMaxTimeMillis(100);
        spin(20);
        timer.endIteration();
        // average 20 ms, about 80 ms remaining
        assertTrue(timer.enoughBudgetIteration(0));
        assertFalse(timer.enoughBudgetIteration(90));
        spin(40);
        timer.endIteration();
        // average 30 ms, about 40 ms remaining: not enough for two more iterations
        assertFalse(timer.enoughBudgetIteration(0));
    }

    @Test
    public void copyKeepsStartTimeAndBudget() {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        timer.setMaxTimeMillis(1000);
        spin(50);
        ElapsedCpuTimer copy = timer.copy();
        assertEquals(timer.elapsedMillis(), copy.elapsedMillis(), 5);
        assertEquals(timer.remainingTimeMillis(), copy.remainingTimeMillis(), 5);

        timer.reset();
        assertTrue("copy is independent of the original", copy.elapsedMillis() >= 40);
    }
}
