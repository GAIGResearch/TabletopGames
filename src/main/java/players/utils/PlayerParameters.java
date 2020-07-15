package players.utils;

import core.AbstractParameters;
import core.interfaces.IStateHeuristic;

import java.util.Objects;

public class PlayerParameters extends AbstractParameters {

    // Budget settings
    public int stopType = PlayerConstants.STOP_TIME;
    public int numIterations = 200;
    public int numFmCalls = 4000;
    public int numTime = 100; //milliseconds

    // Heuristic
    public IStateHeuristic gameHeuristic;

    public PlayerParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return new PlayerParameters(System.currentTimeMillis());
    }

    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerParameters)) return false;
        if (!super.equals(o)) return false;
        PlayerParameters that = (PlayerParameters) o;
        return stopType == that.stopType &&
                numIterations == that.numIterations &&
                numFmCalls == that.numFmCalls &&
                numTime == that.numTime &&
                Objects.equals(gameHeuristic, that.gameHeuristic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stopType, numIterations, numFmCalls, numTime, gameHeuristic);
    }

    @Override
    public AbstractParameters copy() {
        PlayerParameters copy = (PlayerParameters) _copy();
        copy.stopType = stopType;
        copy.numIterations = numIterations;
        copy.numFmCalls = numFmCalls;
        copy.numTime = numTime;
        copy.gameHeuristic = gameHeuristic;
        copy.randomSeed = System.currentTimeMillis();
        return copy;
    }
}
