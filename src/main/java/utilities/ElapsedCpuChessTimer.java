package utilities;

public class ElapsedCpuChessTimer extends ElapsedCpuTimer {

    private long timeRemaining;
    // increments are held in nanoseconds
    private final double incrementAction, incrementTurn, incrementRound, incrementMilestone;

    /**
     * @param maxTimeMillis       - total thinking time for the game
     * @param incrementActionS    - seconds added after each action
     * @param incrementTurnS      - seconds added at the end of each turn
     * @param incrementRoundS     - seconds added at the end of each round
     * @param incrementMilestoneS - seconds added after a game-specific milestone
     */
    public ElapsedCpuChessTimer(long maxTimeMillis, double incrementActionS, double incrementTurnS, double incrementRoundS,
                                double incrementMilestoneS) {
        setMaxTimeMillis(maxTimeMillis);
        this.incrementAction = incrementActionS * 1000000000;
        this.incrementTurn = incrementTurnS * 1000000000;
        this.incrementRound = incrementRoundS * 1000000000;
        this.incrementMilestone = incrementMilestoneS * 1000000000;
        reset();
    }

    private ElapsedCpuChessTimer(ElapsedCpuChessTimer other) {
        this.maxTime = other.maxTime;
        this.oldTime = other.oldTime;
        this.nIters = other.nIters;
        this.incrementAction = other.incrementAction;
        this.incrementTurn = other.incrementTurn;
        this.incrementRound = other.incrementRound;
        this.incrementMilestone = other.incrementMilestone;
        this.timeRemaining = other.timeRemaining;
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

    /**
     * Only meaningful after pause(), and before the increment for the action just taken is added.
     */
    public boolean exceededMaxTime() {
        return timeRemaining <= 0;
    }

    public ElapsedCpuChessTimer copy() {
        return new ElapsedCpuChessTimer(this);
    }

    @Override
    public String toString() {
        return remainingTimeMillis() + " ms remaining (" + incrementAction/1000000.0 + " ms) increment act";
    }
}
