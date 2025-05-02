package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanParameters.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Player may trade any 4 resources of the same type of 1 resource of choice with the bank
 *  This action also includes the Harbor trades using the exchangeRate (2:1, 3:1 etc.)
 */
public class DeepDefaultTrade extends AbstractAction implements IExtendedSequence {
    public final Resource resourceOffer;
    public final int exchangeRate;
    public final int player;

    boolean executed;

    public DeepDefaultTrade(Resource resourceOffer, int exchangeRate, int player){
        this.resourceOffer = resourceOffer;
        this.exchangeRate = exchangeRate;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;
        for (CatanParameters.Resource resToGet: CatanParameters.Resource.values()) {
            if (resToGet != Resource.WILD && resourceOffer != resToGet && gs.getResourcePool().get(resToGet).getValue() > 0) {
                actions.add(new DefaultTrade(resourceOffer, resToGet, exchangeRate, player));
            }
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepDefaultTrade copy() {
        DeepDefaultTrade cp = new DeepDefaultTrade(resourceOffer, exchangeRate, player);
        cp.executed = executed;
        return cp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepDefaultTrade)) return false;
        DeepDefaultTrade that = (DeepDefaultTrade) o;
        return exchangeRate == that.exchangeRate && player == that.player && executed == that.executed && resourceOffer == that.resourceOffer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOffer, exchangeRate, player, executed);
    }

    @Override
    public String toString() {
        return "p" + player + " exchanges " + exchangeRate + " " + resourceOffer + " for 1 ?";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
