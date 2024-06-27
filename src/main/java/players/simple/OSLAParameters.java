package players.simple;

import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.heuristics.PureScoreHeuristic;
import players.heuristics.StateHeuristicType;

import java.util.Arrays;
import java.util.Random;

public class OSLAParameters extends PlayerParameters {
    public IStateHeuristic heuristic;

    public OSLAParameters() {
        addTunableParameter("heuristic",
                StateHeuristicType.PureScoreHeuristic.getExemplarHeuristic(),
                Arrays.stream(StateHeuristicType.values()).map(StateHeuristicType::getExemplarHeuristic).toList());
    }
    @Override
    public void _reset() {
        super._reset();
    }

    @Override
    protected PlayerParameters _copy() {
        OSLAParameters p = new OSLAParameters();
        return p;
    }

    @Override
    public OSLAPlayer instantiate() {
        return new OSLAPlayer(new Random(), this);
    }
    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }
}
