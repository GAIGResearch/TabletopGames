package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.HashMap;
import java.util.Objects;

public class AcceptTrade extends AbstractAction {
    public final int offeringPlayer;
    public final int receivingPlayer;
    public final HashMap<CatanParameters.Resource, Integer> resourcesRequested;
    public final HashMap<CatanParameters.Resource, Integer> resourcesOffered;

    public AcceptTrade(int offeringPlayer, int receivingPlayer,
                       HashMap<CatanParameters.Resource, Integer> resourcesRequested,
                       HashMap<CatanParameters.Resource, Integer> resourcesOffered) {
        this.offeringPlayer = offeringPlayer;
        this.receivingPlayer = receivingPlayer;
        this.resourcesRequested = resourcesRequested;
        this.resourcesOffered = resourcesOffered;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (((CatanGameState)gs).swapResources(receivingPlayer, offeringPlayer, resourcesOffered) &&
                ((CatanGameState)gs).swapResources(offeringPlayer, receivingPlayer, resourcesRequested)) {
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
        return offeringPlayer == that.offeringPlayer && receivingPlayer == that.receivingPlayer && Objects.equals(resourcesRequested, that.resourcesRequested) && Objects.equals(resourcesOffered, that.resourcesOffered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offeringPlayer, receivingPlayer, resourcesRequested, resourcesOffered);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Player %s accepts trade offered by %d : %s for %s ", receivingPlayer, offeringPlayer,
                OfferPlayerTrade.resourceArrayToString(resourcesRequested), OfferPlayerTrade.resourceArrayToString(resourcesOffered));
    }
}
