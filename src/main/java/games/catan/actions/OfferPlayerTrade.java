package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanParameters.Resource;

import java.util.*;

public class OfferPlayerTrade extends AbstractAction implements IExtendedSequence {
    public enum Stage {
        Offer,  // from offering player
        CounterOffer  // from other player
    }

    public final int offeringPlayerID;
    public final int otherPlayerID;
    public final boolean execute;

    public HashMap<Resource, Integer> resourcesOffered;
    public HashMap<Resource, Integer> resourcesRequested;
    Stage stage;
    int negotiationCount;
    boolean finished;

    public OfferPlayerTrade(Stage stage, HashMap<Resource, Integer> resourcesOffered, HashMap<Resource, Integer> resourcesRequested, int offeringPlayerID, int otherPlayerID, boolean execute){
        this.resourcesOffered = resourcesOffered;
        this.resourcesRequested = resourcesRequested;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.execute = execute;
        this.stage = stage;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (!execute) return true;

        this.stage = Stage.CounterOffer;
        this.negotiationCount = 0;
        return gs.setActionInProgress(this);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;

        // Reject the trade offer
        actions.add(new EndNegotiation());

        // Accept the offer if legal exchange
        if (gs.checkCost(resourcesOffered, offeringPlayerID) && gs.checkCost(resourcesRequested, otherPlayerID)) {
            actions.add(new AcceptTrade(resourcesOffered, resourcesRequested, offeringPlayerID, otherPlayerID));
        }

        // Or adjust offer
        CatanParameters.Resource resourceOffered = resourcesOffered.keySet().iterator().next();
        CatanParameters.Resource resourceRequested = resourcesRequested.keySet().iterator().next();
        int nOffered = resourcesOffered.get(resourceOffered);
        int nRequested = resourcesRequested.get(resourceRequested);
        switch(stage) {
            case Offer:
                // Offering player responds to counter-offer with new offer
                actions.addAll(CatanActionFactory.getPlayerTradeOfferActions(gs, state.getCoreGameParameters().actionSpace,
                        offeringPlayerID, otherPlayerID, true, resourceOffered, resourceRequested, nOffered, nRequested, false, stage));
                break;
            case CounterOffer:
                // Other player responds to offer with counter-offer
                actions.addAll(CatanActionFactory.getPlayerTradeOfferActions(gs, state.getCoreGameParameters().actionSpace,
                        otherPlayerID, offeringPlayerID, false, resourceOffered, resourceRequested, nOffered, nRequested, false, stage));
                break;
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        switch(stage) {
            case Offer:
                return offeringPlayerID;
            case CounterOffer:
                return otherPlayerID;
        }
        return offeringPlayerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        negotiationCount++;
        if (stage == Stage.Offer) stage = Stage.CounterOffer;
        else if (stage == Stage.CounterOffer) stage = Stage.Offer;

        // Finished? if offer was accepted / rejected / rejected by default on too much negotiation steps
        if (action instanceof AcceptTrade || action instanceof EndNegotiation || negotiationCount >= ((CatanParameters) state.getGameParameters()).max_negotiation_count) {
            finished = true;
            ((CatanGameState)state).nTradesThisTurn++;
        } else if (action instanceof OfferPlayerTrade) {
            // Update offer
            resourcesOffered = ((OfferPlayerTrade) action).resourcesOffered;
            resourcesRequested = ((OfferPlayerTrade) action).resourcesRequested;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return finished;
    }

    @Override
    public OfferPlayerTrade copy() {
        OfferPlayerTrade opt = new OfferPlayerTrade(stage, new HashMap<>(resourcesOffered), new HashMap<>(resourcesRequested), offeringPlayerID, otherPlayerID, execute);
        opt.stage = stage;
        opt.negotiationCount = negotiationCount;
        opt.finished = finished;
        return opt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OfferPlayerTrade)) return false;
        OfferPlayerTrade that = (OfferPlayerTrade) o;
        return offeringPlayerID == that.offeringPlayerID && otherPlayerID == that.otherPlayerID && negotiationCount == that.negotiationCount && Objects.equals(resourcesOffered, that.resourcesOffered) && Objects.equals(resourcesRequested, that.resourcesRequested) && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourcesOffered, resourcesRequested, offeringPlayerID, otherPlayerID, stage, negotiationCount);
    }

    @Override
    public String toString() {
        if (stage == Stage.Offer) {
            return String.format("p%d offers trade to p%d : %s for %s", offeringPlayerID, otherPlayerID,
                    resourceArrayToString(resourcesOffered), resourceArrayToString(resourcesRequested));
        } else {
            return String.format("p%d counter-offers p%d : %s for %s", otherPlayerID, offeringPlayerID,
                    resourceArrayToString(resourcesOffered), resourceArrayToString(resourcesRequested));
        }
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
}
