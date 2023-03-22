package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OfferPlayerTrade extends AbstractAction {
    public final HashMap<Resource, Integer> resourcesOffered;
    public final HashMap<Resource, Integer> resourcesRequested;
    public final int offeringPlayerID;
    public final int otherPlayerID;
    public final int negotiationCount;

    public OfferPlayerTrade(HashMap<Resource, Integer> resourcesOffered, HashMap<Resource, Integer> resourcesRequested, int offeringPlayerID, int otherPlayerID, int negotiationCount){
        this.resourcesOffered = resourcesOffered;
        this.resourcesRequested = resourcesRequested;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.negotiationCount = negotiationCount;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        cgs.setCurrentTradeOffer(this);
        return true;
    }

    @Override
    public OfferPlayerTrade copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OfferPlayerTrade)) return false;
        OfferPlayerTrade that = (OfferPlayerTrade) o;
        return offeringPlayerID == that.offeringPlayerID && otherPlayerID == that.otherPlayerID && negotiationCount == that.negotiationCount && Objects.equals(resourcesOffered, that.resourcesOffered) && Objects.equals(resourcesRequested, that.resourcesRequested);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourcesOffered, resourcesRequested, offeringPlayerID, otherPlayerID, negotiationCount);
    }

    @Override
    public String toString() {
        return String.format("Player %d offers trade to player %d : %s for %s", offeringPlayerID, otherPlayerID,
                resourceArrayToString(resourcesOffered), resourceArrayToString(resourcesRequested));
    }

    public static String resourceArrayToString(HashMap<Resource, Integer> resources) {
        StringBuilder retValue = new StringBuilder();
        for (Map.Entry<Resource, Integer> e: resources.entrySet()) {
            if (e.getValue() > 0) {
                if (retValue.length() > 0)
                    retValue.append(", ");
                retValue.append(e.getValue()).append(" ").append(e.getKey());
            }
        }
        return retValue.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getNegotiationCount() {
        return negotiationCount;
    }

    public int getOfferingPlayerID() {
        return offeringPlayerID;
    }

    public int getOtherPlayerID() {
        return otherPlayerID;
    }

    public HashMap<Resource, Integer> getResourcesOffered() {
        return resourcesOffered;
    }

    public HashMap<Resource, Integer> getResourcesRequested() {
        return resourcesRequested;
    }
}
