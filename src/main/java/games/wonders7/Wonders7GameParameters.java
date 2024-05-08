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

    public Wonders7GameParameters(long seed) {
        super(seed);
        addTunableParameter("nWonderCardsPerPlayer", 7);
        _reset();
    }

    @Override
    public void _reset() {
        nWonderCardsPerPlayer = (int) getParameterValue("nWonderCardsPerPlayer");
    }

    @Override
    protected AbstractParameters _copy() {
        Wonders7GameParameters wgp = new Wonders7GameParameters(System.currentTimeMillis());
        wgp.nWonderCardsPerPlayer = nWonderCardsPerPlayer;
        return wgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonders7GameParameters)) return false;
        if (!super.equals(o)) return false;
        Wonders7GameParameters that = (Wonders7GameParameters) o;
        return nWonderCardsPerPlayer == that.nWonderCardsPerPlayer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nWonderCardsPerPlayer);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Wonders7, new Wonders7ForwardModel(), new Wonders7GameState(this, GameType.Wonders7.getMinPlayers()));
    }
}