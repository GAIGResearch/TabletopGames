package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters.Resources;

import java.util.List;

/* Player may trade any 4 resources of the same type of 1 resource of choice with the bank
* This action also includes the Harbor trades using the exchangeRate*/
public class OfferPlayerTrade extends AbstractAction {
    List<Resources> resourcesOffered;
    List<Resources> resourcesRequested;
    int offeringPlayerID;
    int otherPlayerID;
    int negotiationCount = 0;

    public OfferPlayerTrade(List<Resources> resourcesOffered, List<Resources> resourcesRequested, int offeringPlayerID, int otherPlayerID){
        this.resourcesOffered = resourcesOffered;
        this.resourcesRequested = resourcesRequested;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        // todo update logic to handle trade negotiations
        CatanGameState cgs = (CatanGameState)gs;
        negotiationCount++;
        cgs.setCurrentTradeOffer(this);

        return true;
    }

    @Override
    public AbstractAction copy() {
        OfferPlayerTrade other = new OfferPlayerTrade(resourcesOffered, resourcesRequested, offeringPlayerID, otherPlayerID);
        other.negotiationCount = this.negotiationCount;
        return other;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof OfferPlayerTrade){
            OfferPlayerTrade otherAction = (OfferPlayerTrade)other;
            return resourcesOffered == otherAction.resourcesOffered && resourcesRequested == otherAction.resourcesRequested && offeringPlayerID == otherAction.offeringPlayerID && otherPlayerID == otherAction.otherPlayerID && negotiationCount == otherAction.negotiationCount;
        }
        return false;
    }

    public int getOtherPlayerID() {
        return otherPlayerID;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + gameState.getCurrentPlayer() + " offering "+ resourcesOffered.size() + " " + resourcesOffered.get(0) + " in exchange for " + resourcesRequested.size() + " " + resourcesRequested.get(0) + " from player " + otherPlayerID;
    }

    public int getNegotiationCount() {
        return negotiationCount;
    }

}
