package games.conquest.players;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.Arrays;

public class CQMCTSParams extends PlayerParameters {
    public double K = 1.4;
    public int maxTreeDepth = 15;
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public boolean flexibleBudget = true;
    public int rolloutLength = 10;

    public CQMCTSParams() {
        super();
        addTunableParameter("K", 1.4, Arrays.asList(0.0, 0.001, 0.1, 0.5, 1.0, 1.4));
        addTunableParameter("maxTreeDepth", 15, Arrays.asList(15, 20, 25));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("flexibleBudget", true);
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 5, 10, 15));
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    @Override
    protected CQMCTSParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new CQMCTSParams();
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public CQMCTSPlayer instantiate() {
        return new CQMCTSPlayer((CQMCTSParams) this.copy());
    }
}
