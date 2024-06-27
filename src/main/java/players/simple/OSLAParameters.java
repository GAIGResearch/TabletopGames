package players.simple;

import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.heuristics.StateHeuristicType;

import java.util.Arrays;
import java.util.Random;

public class OSLAParameters extends PlayerParameters {
    public StateHeuristicType heuristic = StateHeuristicType.PureScoreHeuristic;

    public IStateHeuristic heuristicFunc;

    public OSLAParameters() {
        addTunableParameter("heuristic", StateHeuristicType.PureScoreHeuristic, Arrays.asList(StateHeuristicType.values()));
    }
    @Override
    public void _reset() {
        super._reset();
        if (heuristic != getParameterValue("heuristic")) {
            heuristic = (StateHeuristicType) getParameterValue("heuristic");
            heuristicFunc = heuristic.getHeuristic();
        }
    }

    @Override
    protected PlayerParameters _copy() {
        OSLAParameters p = new OSLAParameters();
        p.heuristicFunc = heuristicFunc;
        return p;
    }

    @Override
    public OSLAPlayer instantiate() {
        return new OSLAPlayer(new Random(), this);
    }

    public IStateHeuristic getHeuristic() {
        return heuristicFunc;
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return getHeuristic();
    }
}
