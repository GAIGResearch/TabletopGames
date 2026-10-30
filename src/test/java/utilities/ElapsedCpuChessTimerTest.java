package utilities;

import org.junit.Test;

import static org.junit.Assert.*;
import static utilities.ElapsedCpuTimerTest.spin;

public class ElapsedCpuChessTimerTest {

    @Test
    public void newTimerHasTheFullClock() {
        assertEquals(20_000, new ElapsedCpuChessTimer(20_000, 0, 0, 0, 0).remainingTimeMillis());
        assertEquals(1_500, new ElapsedCpuChessTimer(1_500, 0, 0, 0, 0).remainingTimeMillis());
    }

    @Test
    public void clockOnlyRunsBetweenResumeAndPause() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(20_000, 0, 0, 0, 0);
        spin(30);
        assertEquals("not yet resumed", 20_000, timer.remainingTimeMillis());

        timer.resume();
        spin(50);
        timer.pause();
        long used = 20_000 - timer.remainingTimeMillis();
        assertTrue("used " + used, used >= 40 && used < 1000);

        long remaining = timer.remainingTime();
        spin(50);
        assertEquals("paused", remaining, timer.remainingTime());
    }

    @Test
    public void timeAsleepCountsOnlyOnWindows() throws InterruptedException {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(20_000, 0, 0, 0, 0);
        timer.resume();
        Thread.sleep(50);
        timer.pause();
        long used = 20_000 - timer.remainingTimeMillis();
        if (ElapsedCpuTimer.CPU_TIME)
            assertTrue("used " + used, used < 20);
        else
            assertTrue("used " + used, used >= 45);
    }

    @Test
    public void incrementsAreGivenInSeconds() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(1_000, 1.5, 2, 3, 0.25);
        timer.incrementAction();
        assertEquals(2_500, timer.remainingTimeMillis());
        timer.incrementTurn();
        assertEquals(4_500, timer.remainingTimeMillis());
        timer.incrementRound();
        assertEquals(7_500, timer.remainingTimeMillis());
        timer.incrementMileStone();
        assertEquals(7_750, timer.remainingTimeMillis());
    }

    @Test
    public void copyKeepsIncrements() {
        ElapsedCpuChessTimer copy = new ElapsedCpuChessTimer(1_000, 1, 1, 1, 1).copy();
        assertEquals(1_000, copy.remainingTimeMillis());
        copy.incrementAction();
        copy.incrementTurn();
        copy.incrementRound();
        copy.incrementMileStone();
        assertEquals(5_000, copy.remainingTimeMillis());
    }

    @Test
    public void copyKeepsRemainingTimeAndIsIndependent() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(10_000, 1, 0, 0, 0);
        timer.resume();
        spin(30);
        timer.pause();
        ElapsedCpuChessTimer copy = timer.copy();
        assertEquals(timer.remainingTime(), copy.remainingTime());

        copy.incrementAction();
        assertEquals(timer.remainingTime() + 1_000_000_000L, copy.remainingTime());

        copy.resume();
        spin(30);
        copy.pause();
        assertTrue(copy.remainingTime() < timer.remainingTime() + 1_000_000_000L);
    }

    @Test
    public void copyOfRunningTimerKeepsItsStartTime() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(10_000, 0, 0, 0, 0);
        timer.resume();
        spin(50);
        ElapsedCpuChessTimer copy = timer.copy();
        copy.pause();
        assertTrue(10_000 - copy.remainingTimeMillis() >= 40);
    }

    @Test
    public void exceededMaxTimeOnlyWhenTheClockHasRunOut() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(30, 0, 0, 0, 0);
        timer.resume();
        timer.pause();
        assertFalse(timer.exceededMaxTime());

        timer.resume();
        spin(50);
        timer.pause();
        assertTrue(timer.exceededMaxTime());
    }

    @Test
    public void largeIncrementDoesNotMakeAFreshClockLookExceeded() {
        ElapsedCpuChessTimer timer = new ElapsedCpuChessTimer(1_000, 5, 0, 0, 0);
        assertFalse(timer.exceededMaxTime());
        assertFalse(timer.copy().exceededMaxTime());
    }
}
