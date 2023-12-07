package games.wonders7;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Objects;

public class Wonders7GameParameters extends TunableParameters {
    public int nWonderCardsPerPlayer = 7;
    public int nCostNeighbourResource = 2;
    public int nCoinsDiscard = 3;
    public int startingCoins = 3;

    public Wonders7GameParameters() {
        addTunableParameter("nWonderCardsPerPlayer", 7);
        addTunableParameter("nCostNeighbourResource", 2);
        addTunableParameter("nCoinsDiscard", 3);
        addTunableParameter("startingCoins", 3);
        _reset();
    }

    @Override
    public void _reset() {
        nWonderCardsPerPlayer = (int) getParameterValue("nWonderCardsPerPlayer");
        nCostNeighbourResource = (int) getParameterValue("nCostNeighbourResource");
        nCoinsDiscard = (int) getParameterValue("nCoinsDiscard");
        startingCoins = (int) getParameterValue("startingCoins");
    }

    @Override
    protected AbstractParameters _copy() {
        return new Wonders7GameParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonders7GameParameters)) return false;
        Wonders7GameParameters that = (Wonders7GameParameters) o;
        return nWonderCardsPerPlayer == that.nWonderCardsPerPlayer && nCostNeighbourResource == that.nCostNeighbourResource &&
                nCoinsDiscard == that.nCoinsDiscard && startingCoins == that.startingCoins;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Wonders7, new Wonders7ForwardModel(), new Wonders7GameState(this, GameType.Wonders7.getMinPlayers()));
    }
}