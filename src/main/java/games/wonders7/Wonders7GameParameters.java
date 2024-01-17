package games.wonders7;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

public class Wonders7GameParameters extends TunableParameters {
    public int nWonderCardsPerPlayer = 7;
    public int nCostNeighbourResource = 2;
    public int nCoinsDiscard = 3;
    public int startingCoins = 3;

    // if either wonder or card distribution seeds are set to something other than -1,
    // then this seed is fixed. The game random seed will be used in all cases where these are -1 (the default)
    public int wonderShuffleSeed = -1;
    public int cardShuffleSeed = -1;

    public Wonders7GameParameters() {
        addTunableParameter("nWonderCardsPerPlayer", 7);
        addTunableParameter("nCostNeighbourResource", 2);
        addTunableParameter("nCoinsDiscard", 3);
        addTunableParameter("startingCoins", 3);
        addTunableParameter("wonderShuffleSeed", -1);
        addTunableParameter("cardShuffleSeed", -1);
        _reset();
    }

    @Override
    public void _reset() {
        nWonderCardsPerPlayer = (int) getParameterValue("nWonderCardsPerPlayer");
        nCostNeighbourResource = (int) getParameterValue("nCostNeighbourResource");
        nCoinsDiscard = (int) getParameterValue("nCoinsDiscard");
        startingCoins = (int) getParameterValue("startingCoins");
        wonderShuffleSeed = (int) getParameterValue("wonderShuffleSeed");
        cardShuffleSeed = (int) getParameterValue("cardShuffleSeed");
    }

    @Override
    protected AbstractParameters _copy() {
        return new Wonders7GameParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return (o instanceof Wonders7GameParameters);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Wonders7, new Wonders7ForwardModel(), new Wonders7GameState(this, GameType.Wonders7.getMinPlayers()));
    }
}