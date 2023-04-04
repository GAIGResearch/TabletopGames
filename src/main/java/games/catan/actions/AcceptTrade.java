package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.HashMap;
import java.util.Objects;

public class AcceptTrade extends AbstractAction {
    public final int playerID;
    public final int offeringPlayer;
    public final int otherPlayer;
    public final HashMap<CatanParameters.Resource, Integer> resourcesRequested;
    public final HashMap<CatanParameters.Resource, Integer> resourcesOffered;

    public AcceptTrade(int playerID, HashMap<CatanParameters.Resource, Integer> resourcesOffered,
                       HashMap<CatanParameters.Resource, Integer> resourcesRequested,
                       int offeringPlayerID, int otherPlayerID) {
        this.playerID = playerID;
        this.offeringPlayer = offeringPlayerID;
        this.otherPlayer = otherPlayerID;
        this.resourcesRequested = resourcesRequested;
        this.resourcesOffered = resourcesOffered;
    }
    public AcceptTrade(int playerID, CatanParameters.Resource resourceOffered, int nOffered,
                       CatanParameters.Resource resourceRequested, int nRequested,
                       int offeringPlayerID, int otherPlayerID) {
        this.playerID = playerID;
        this.offeringPlayer = offeringPlayerID;
        this.otherPlayer = otherPlayerID;
        this.resourcesRequested = new HashMap<CatanParameters.Resource, Integer>() {{ put(resourceRequested, nRequested); }};
        this.resourcesOffered = new HashMap<CatanParameters.Resource, Integer>() {{ put(resourceOffered, nOffered); }};
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (((CatanGameState)gs).swapResources(otherPlayer, offeringPlayer, resourcesRequested) &&
                ((CatanGameState)gs).swapResources(offeringPlayer, otherPlayer, resourcesOffered)) {
            ((CatanGameState) gs).setTradeOffer(null);
            gs.setTurnOwner(offeringPlayer);
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
        return playerID == that.playerID && offeringPlayer == that.offeringPlayer && otherPlayer == that.otherPlayer && Objects.equals(resourcesRequested, that.resourcesRequested) && Objects.equals(resourcesOffered, that.resourcesOffered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, offeringPlayer, otherPlayer, resourcesRequested, resourcesOffered);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("p%s accepts trade by p%d to p%d : %s for %s ", playerID, offeringPlayer, otherPlayer,
                OfferPlayerTrade.resourceArrayToString(resourcesOffered), OfferPlayerTrade.resourceArrayToString(resourcesRequested));
    }
}
