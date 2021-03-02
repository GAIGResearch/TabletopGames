package utilities;

public class ElapsedCpuChessTimer extends ElapsedCpuTimer {

    private long timeRemaining;
    private final long increment;

    public ElapsedCpuChessTimer(long maxTimeMinutes, long incrementSeconds) {
        setMaxTimeMillis(maxTimeMinutes * 60000);
        this.increment = incrementSeconds * 1000000000;
        reset();
    }

    public void reset() {
        super.reset();
        timeRemaining = maxTime;
    }

    public void pause() {
        // Update timeRemaining variable with time elapsed
        timeRemaining -= elapsed();
        // Add increment
        timeRemaining += increment;
    }

    public void resume() {
        // Update oldTime to current time
        oldTime = getTime();
    }

    public long remainingTime() {
        return timeRemaining;
    }

    public long remainingTimeMillis() {
        return (long) (timeRemaining / 1000000.0);
    }

    public boolean exceededMaxTime() {
        return (timeRemaining - increment) <= 0;
    }

    public ElapsedCpuChessTimer copy()
    {
        ElapsedCpuChessTimer newCpuTimer = new ElapsedCpuChessTimer(this.maxTime, this.increment);
        newCpuTimer.oldTime = this.oldTime;
        newCpuTimer.bean = this.bean;
        newCpuTimer.nIters = this.nIters;
        newCpuTimer.timeRemaining = this.timeRemaining;
        return newCpuTimer;
    }

    @Override
    public String toString() {
        return remainingTimeMillis() + " ms remaining (" + increment/1000000.0 + " ms) increment";
    }
}