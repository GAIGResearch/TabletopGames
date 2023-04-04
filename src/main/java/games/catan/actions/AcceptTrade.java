package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.HashMap;
import java.util.Objects;

public class AcceptTrade extends AbstractAction {
    public final int offeringPlayer;
    public final int otherPlayer;
    public final HashMap<CatanParameters.Resource, Integer> resourcesRequested;
    public final HashMap<CatanParameters.Resource, Integer> resourcesOffered;

    public AcceptTrade(HashMap<CatanParameters.Resource, Integer> resourcesOffered,
                       HashMap<CatanParameters.Resource, Integer> resourcesRequested,
                       int offeringPlayerID, int otherPlayerID) {
        this.offeringPlayer = offeringPlayerID;
        this.otherPlayer = otherPlayerID;
        this.resourcesRequested = resourcesRequested;
        this.resourcesOffered = resourcesOffered;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (((CatanGameState)gs).swapResources(otherPlayer, offeringPlayer, resourcesRequested) &&
                ((CatanGameState)gs).swapResources(offeringPlayer, otherPlayer, resourcesOffered)) {
            return true;
        } else {
            throw new AssertionError("A partner did not have sufficient resources");
        }
    }

    @Override
    public AcceptTrade copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcceptTrade)) return false;
        AcceptTrade that = (AcceptTrade) o;
        return offeringPlayer == that.offeringPlayer && otherPlayer == that.otherPlayer && Objects.equals(resourcesRequested, that.resourcesRequested) && Objects.equals(resourcesOffered, that.resourcesOffered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offeringPlayer, otherPlayer, resourcesRequested, resourcesOffered);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("p%s accepts trade by p%d : %s for %s ", otherPlayer, offeringPlayer,
                OfferPlayerTrade.resourceArrayToString(resourcesOffered), OfferPlayerTrade.resourceArrayToString(resourcesRequested));
    }
}
