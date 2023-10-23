package groupM.players.breadthFirst;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

public class BreadthFirstParams extends PlayerParameters {

    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public BreadthFirstParams() {
        super(System.currentTimeMillis());
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    protected BreadthFirstParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        BreadthFirstParams params =  new BreadthFirstParams();
        params.setRandomSeed(super.getRandomSeed());
        return params;
    }

    @Override
    public void _reset() {
        super._reset();
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    @Override
    public BreadthFirstPlayer instantiate() {
        return new BreadthFirstPlayer((BreadthFirstParams) this.copy());
    }
}
