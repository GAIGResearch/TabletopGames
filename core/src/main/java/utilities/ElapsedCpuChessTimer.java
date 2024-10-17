package utilities;

public class ElapsedCpuChessTimer extends ElapsedCpuTimer {

    private long timeRemaining;
    private final double incrementAction, incrementTurn, incrementRound, incrementMilestone;

    public ElapsedCpuChessTimer(long maxTimeMinutes, double incrementAction, double incrementTurn, double incrementRound,
                                double incrementMilestone) {
        setMaxTimeMillis(maxTimeMinutes * 60000);
        this.incrementAction = incrementAction * 1000000000;
        this.incrementTurn = incrementTurn * 1000000000;
        this.incrementRound = incrementRound * 1000000000;
        this.incrementMilestone = incrementMilestone * 1000000000;
        reset();
    }

    public void reset() {
        super.reset();
        timeRemaining = maxTime;
    }

    public void pause() {
        // Update timeRemaining variable with time elapsed
        timeRemaining -= elapsed();
    }

    public void incrementAction() {
        // Add increment
        timeRemaining += incrementAction;
    }

    public void incrementTurn() {
        // Add increment
        timeRemaining += incrementTurn;
    }

    public void incrementRound() {
        // Add increment
        timeRemaining += incrementRound;
    }

    public void incrementMileStone() {
        // Add increment
        timeRemaining += incrementMilestone;
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
        return (timeRemaining - incrementAction) <= 0;
    }

    public ElapsedCpuChessTimer copy()
    {
        ElapsedCpuChessTimer newCpuTimer = new ElapsedCpuChessTimer(this.maxTime, this.incrementAction,
                this.incrementTurn, this.incrementRound, this.incrementMilestone);
        newCpuTimer.oldTime = this.oldTime;
        newCpuTimer.bean = this.bean;
        newCpuTimer.nIters = this.nIters;
        newCpuTimer.timeRemaining = this.timeRemaining;
        return newCpuTimer;
    }

    @Override
    public String toString() {
        return remainingTimeMillis() + " ms remaining (" + incrementAction/1000000.0 + " ms) increment act";
    }
}