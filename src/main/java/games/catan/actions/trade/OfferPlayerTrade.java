package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters.Resource;

import java.util.*;

public class OfferPlayerTrade extends AbstractAction {
    public enum Stage {
        Offer,  // from offering player
        CounterOffer  // from other player
    }

    public final int offeringPlayerID;
    public final int otherPlayerID;
    public final Resource resourceOffered;
    public final int nOffered;
    public final Resource resourceRequested;
    public final int nRequested;
    public final Stage stage;

    public OfferPlayerTrade(Stage stage, Resource resourceOffered, int nOffered, Resource resourceRequested, int nRequested,
                            int offeringPlayerID, int otherPlayerID){
        this.resourceOffered = resourceOffered;
        this.nOffered = nOffered;
        this.resourceRequested = resourceRequested;
        this.nRequested = nRequested;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.stage = stage;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((CatanGameState)gs).setTradeOffer(this);
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
        return offeringPlayerID == that.offeringPlayerID && otherPlayerID == that.otherPlayerID && nOffered == that.nOffered && nRequested == that.nRequested && resourceOffered == that.resourceOffered && resourceRequested == that.resourceRequested && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offeringPlayerID, otherPlayerID, resourceOffered, nOffered, resourceRequested, nRequested, stage);
    }

    @Override
    public String toString() {
        // no String.format, which is slow, as the history records this text for every offer made in a search
        return "Trade (p" + offeringPlayerID + " to p" + otherPlayerID + " : " + nOffered + " " + resourceOffered +
                " for " + nRequested + " " + resourceRequested + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
