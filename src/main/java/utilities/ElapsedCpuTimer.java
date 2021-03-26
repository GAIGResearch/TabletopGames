package utilities;

/**
 * Created by diego on 26/02/14.
 */


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class ElapsedCpuTimer {

    protected static final boolean OS_WIN = System.getProperty("os.name").contains("Windows");

    // allows for easy reporting of elapsed time
    protected ThreadMXBean bean = ManagementFactory.getThreadMXBean();
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
        newCpuTimer.bean = this.bean;
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
        return getCpuTime();
    }

    protected long getCpuTime() {
        if(OS_WIN)
            return System.nanoTime();

        if (bean.isCurrentThreadCpuTimeSupported()) {
            return bean.getCurrentThreadCpuTime();
        } else {
            throw new RuntimeException("CpuTime NOT Supported");
        }
    }

}