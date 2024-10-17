package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters.Resource;

import java.util.Objects;

/**
 *  Player may trade any 4 resources of the same type of 1 resource of choice with the bank
 *  This action also includes the Harbor trades using the exchangeRate (2:1, 3:1 etc.)
 */
public class DefaultTrade extends AbstractAction {
    public final Resource resourceOffer;
    public final Resource resourceToGet;
    public final int exchangeRate;
    public final int player;

    public DefaultTrade(Resource resourceOffer, Resource resourceToGet, int exchangeRate, int player){
        this.resourceOffer = resourceOffer;
        this.resourceToGet = resourceToGet;
        this.exchangeRate = exchangeRate;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        if (cgs.getPlayerResources(player).get(resourceOffer).getValue() < exchangeRate) throw new AssertionError("Player does not have enough resources for this trade");
        cgs.getPlayerResources(player).get(resourceOffer).decrement(exchangeRate);
        cgs.getPlayerResources(player).get(resourceToGet).increment();
        return true;
    }

    @Override
    public DefaultTrade copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultTrade)) return false;
        DefaultTrade that = (DefaultTrade) o;
        return exchangeRate == that.exchangeRate && player == that.player && resourceOffer == that.resourceOffer && resourceToGet == that.resourceToGet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOffer, resourceToGet, exchangeRate, player);
    }

    @Override
    public String toString() {
        return "p" + player + " exchanges " + exchangeRate + " " + resourceOffer + " for 1 " + resourceToGet;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
