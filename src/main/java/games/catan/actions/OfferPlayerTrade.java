package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters.Resources;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OfferPlayerTrade extends AbstractAction {
    public final int[] resourcesOffered;
    public final int[] resourcesRequested;
    public final int offeringPlayerID;
    public final int otherPlayerID;
    public final int negotiationCount;

    public OfferPlayerTrade(int[] resourcesOffered, int[] resourcesRequested, int offeringPlayerID, int otherPlayerID, int negotiationCount){
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
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OfferPlayerTrade){
            OfferPlayerTrade otherAction = (OfferPlayerTrade)other;
            return Arrays.equals(otherAction.resourcesRequested, resourcesRequested)
                    && Arrays.equals(otherAction.resourcesOffered, resourcesOffered)
                    && offeringPlayerID == otherAction.offeringPlayerID
                    && otherPlayerID == otherAction.otherPlayerID
                    && negotiationCount == otherAction.negotiationCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(offeringPlayerID,otherPlayerID,negotiationCount);
        return retValue + 17 * Arrays.hashCode(resourcesOffered) + 73 * Arrays.hashCode(resourcesRequested);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        //todo expand this string method
        return "Player " + offeringPlayerID + " offering trade to player "
                + otherPlayerID;

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

    public int[] getResourcesOffered() {
        return resourcesOffered;
    }

    public int[] getResourcesRequested() {
        return resourcesRequested;
    }


}
