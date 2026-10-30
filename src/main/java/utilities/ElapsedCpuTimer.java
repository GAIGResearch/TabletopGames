package utilities;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Created by diego on 26/02/14.
 * <p>
 * This measures the CPU time of the current thread, so excludes garbage collection pauses and time when the thread
 * is not scheduled. A timer must therefore be read on the thread that started it.
 * The exception is Windows, where thread CPU time only advances in steps of the scheduler tick (15.6 ms), which is
 * too coarse for a per-move budget; there it measures wall-clock time (System.nanoTime()).
 */
public class ElapsedCpuTimer {

    private static final ThreadMXBean BEAN = ManagementFactory.getThreadMXBean();
    public static final boolean CPU_TIME = !System.getProperty("os.name").contains("Windows")
            && BEAN.isCurrentThreadCpuTimeSupported();

    protected long oldTime;
    protected long maxTime;
    protected int nIters;

    public ElapsedCpuTimer() {
        reset();
    }

    public void reset() {
        oldTime = getTime();
        nIters = 0;
    }

    public long elapsed() {
        return getTime() - oldTime;
    }

    public long elapsedNanos() {
        return elapsed();
    }

    public long elapsedMillis() {
        return (long) (elapsed() / 1000000.0);
    }

    public double elapsedSeconds() {
        return elapsedMillis()/1000.0;
    }

    public double elapsedMinutes() {
        return elapsedMillis()/1000.0/60.0;
    }

    public double elapsedHours() {
        return elapsedMinutes()/60.0;
    }

    public void setMaxTimeMillis(long time) {
        maxTime = time * 1000000;
    }

    public long remainingTimeMillis() {
        long diff = maxTime - elapsed();
        return (long) (diff / 1000000.0);
    }

    public long remainingTimeNanos() {
        return maxTime - elapsed();
    }

    public boolean exceededMaxTime() {
        return elapsed() > maxTime;
    }

    /**
     * Calculates average time spent per iteration.
     * @param break_ms - optional parameter to add a safety check for early stopping, can be 0.
     * @return - true if enough budget is left for another iteration, false otherwise.
     */
    public boolean enoughBudgetIteration(int break_ms) {
        long average;
        if (nIters == 0) average = 0; else average = elapsedMillis() / nIters;
        long remaining = remainingTimeMillis();
        return remaining > 2 * average && remaining > break_ms;
    }

    /**
     * We finished an iteration, so increasing the iteration count.
     */
    public void endIteration() {
        nIters++;
    }

    public ElapsedCpuTimer copy()
    {
        ElapsedCpuTimer newCpuTimer = new ElapsedCpuTimer();
        newCpuTimer.maxTime = this.maxTime;
        newCpuTimer.oldTime = this.oldTime;
        newCpuTimer.nIters = this.nIters;
        return newCpuTimer;
    }

    @Override
    public String toString() {
        // now resets the timer...
        String ret = elapsed() / 1000000.0 + " ms elapsed";
        //reset();
        return ret;
    }

    protected long getTime() {
        return CPU_TIME ? BEAN.getCurrentThreadCpuTime() : System.nanoTime();
    }

}