package players.DUCT;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;

public class DUCTParams extends PlayerParameters {

    public double K = 1.0;
    public int rolloutLength = 10;
    public int maxTreeDepth = 20;
    public double firstPlayUrgency = 1_000_000.0; // score for an untried action so it gets picked first
    public boolean normaliseRewards = true;       // scale scores to [0,1] using min/max seen so far
    public boolean redeterminise = true;          // re-guess the hidden cards each iteration
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public DUCTParams() {
        addTunableParameter("K", 1.0, Arrays.asList(0.03, 0.1, 0.3, 1.0, 3.0, 10.0, 30.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100, 1000));
        addTunableParameter("maxTreeDepth", 20, Arrays.asList(3, 10, 20, 50, 100, 1000));
        addTunableParameter("firstPlayUrgency", 1_000_000.0);
        addTunableParameter("normaliseRewards", true);
        addTunableParameter("redeterminise", true, Arrays.asList(false, true));
        addTunableParameter("heuristic", IStateHeuristic.class, AbstractGameState::getHeuristicScore);
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        firstPlayUrgency = (double) getParameterValue("firstPlayUrgency");
        normaliseRewards = (boolean) getParameterValue("normaliseRewards");
        redeterminise = (boolean) getParameterValue("redeterminise");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    @Override
    protected DUCTParams _copy() {
        return new DUCTParams();
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof DUCTParams other)) return false;
        return Double.compare(K, other.K) == 0
                && rolloutLength == other.rolloutLength
                && maxTreeDepth == other.maxTreeDepth
                && Double.compare(firstPlayUrgency, other.firstPlayUrgency) == 0
                && normaliseRewards == other.normaliseRewards
                && redeterminise == other.redeterminise;
    }

    @Override
    public BasicDUCTPlayer instantiate() {
        return new BasicDUCTPlayer((DUCTParams) this.copy());
    }
}
