package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanParameters.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.catan.actions.trade.DeepConstructNewOffer.Choice.*;

public class DeepConstructNewOffer extends AbstractAction implements IExtendedSequence {
    public enum Choice {
        ChoosePlayerTradeWith,
        ChooseResourceOffer,
        ChooseNOffered,
        ChooseResourceRequested,
        ChooseNRequested,
        OfferComplete
    }

    public final int player;

    public final int offeringPlayerID;
    public final boolean execute;
    public final OfferPlayerTrade.Stage stage;

    public int otherPlayerID;
    public Resource resourceOffered;
    public int nOffered;
    public Resource resourceRequested;
    public int nRequested;
    Choice choice;

    // 1. Create action. Choose player to trade with next.
    public DeepConstructNewOffer(int player) {
        this.player = player;
        this.offeringPlayerID = player;
        this.execute = true;
        this.stage = OfferPlayerTrade.Stage.Offer;
        this.choice = ChoosePlayerTradeWith;
    }

    // 2. Choose resource to offer
    public DeepConstructNewOffer(int player, int otherPlayerID) {
        this.player = player;
        this.offeringPlayerID = player;
        this.otherPlayerID = otherPlayerID;
        this.execute = false;
        this.stage = OfferPlayerTrade.Stage.Offer;
        this.choice = ChooseResourceOffer;
    }
    // 3. Choose how many offered (in offer or counter-offer)
    public DeepConstructNewOffer(int player, OfferPlayerTrade.Stage stage, int offeringPlayerID, int otherPlayerID, Resource resourceOffered) {
        this.player = player;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.resourceOffered = resourceOffered;
        this.execute = false;
        this.stage = stage;
        this.choice = ChooseNOffered;
    }
    // 4. Choose resource to get
    public DeepConstructNewOffer(int player, int otherPlayerID, Resource resourceOffered, int nOffered) {
        this.player = player;
        this.offeringPlayerID = player;
        this.otherPlayerID = otherPlayerID;
        this.resourceOffered = resourceOffered;
        this.nOffered = nOffered;
        this.execute = false;
        this.stage = OfferPlayerTrade.Stage.Offer;
        this.choice = ChooseResourceRequested;
    }
    // 5. Choose how many requested (in offer or counter-offer)
    public DeepConstructNewOffer(int player, OfferPlayerTrade.Stage stage, int offeringPlayerID, int otherPlayerID, Resource resourceOffered, int nOffered, Resource resourceRequested) {
        this.player = player;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.resourceOffered = resourceOffered;
        this.resourceRequested = resourceRequested;
        this.nOffered = nOffered;
        this.execute = false;
        this.stage = stage;
        this.choice = ChooseNRequested;
    }
    // 6. Offer complete
    public DeepConstructNewOffer(int player, OfferPlayerTrade.Stage stage, int offeringPlayerID, int otherPlayerID, Resource resourceOffered, int nOffered, Resource resourceRequested, int nRequested) {
        this.player = player;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.resourceOffered = resourceOffered;
        this.resourceRequested = resourceRequested;
        this.nRequested = nRequested;
        this.nOffered = nOffered;
        this.execute = false;
        this.stage = stage;
        this.choice = OfferComplete;
    }

    // Copy constructor
    public DeepConstructNewOffer(int player, OfferPlayerTrade.Stage stage, int offeringPlayerID, int otherPlayerID, Resource resourceOffered, int nOffered, Resource resourceRequested, int nRequested, boolean execute, Choice choice) {
        this.player = player;
        this.offeringPlayerID = offeringPlayerID;
        this.otherPlayerID = otherPlayerID;
        this.resourceOffered = resourceOffered;
        this.resourceRequested = resourceRequested;
        this.nRequested = nRequested;
        this.nOffered = nOffered;
        this.execute = execute;
        this.stage = stage;
        this.choice = choice;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (!execute) return true;
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;
        // Fill in the respective choice
        switch(choice) {
            case ChoosePlayerTradeWith:
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    if (i != offeringPlayerID && gs.getNResourcesInHand(i) > 0) {
                        actions.add(new DeepConstructNewOffer(offeringPlayerID, i));
                    }
                }
                break;
            case ChooseResourceOffer:
                for (Resource res: Resource.values()) {
                    if (res == Resource.WILD) continue;
                    if (gs.getPlayerResources(offeringPlayerID).get(res).getValue() == 0) continue;
                    actions.add(new DeepConstructNewOffer(player, OfferPlayerTrade.Stage.Offer, offeringPlayerID, otherPlayerID, res));
                }
                break;
            case ChooseNOffered:
                int nAvailableOffer = stage == OfferPlayerTrade.Stage.Offer? gs.getPlayerResources(offeringPlayerID).get(resourceOffered).getValue() : ((CatanParameters)gs.getGameParameters()).max_resources_request_trade;
                for (int i = 1; i <= nAvailableOffer; i++) {
                    actions.add(new DeepConstructNewOffer(player, otherPlayerID, resourceOffered, i));
                }
                break;
            case ChooseResourceRequested:
                for (Resource res: Resource.values()) {
                    if (res == Resource.WILD) continue;
                    actions.add(new DeepConstructNewOffer(player, OfferPlayerTrade.Stage.Offer, offeringPlayerID, otherPlayerID, resourceOffered, nOffered, res));
                }
                break;
            case ChooseNRequested:
                int nAvailableRequest = stage == OfferPlayerTrade.Stage.CounterOffer? gs.getPlayerResources(otherPlayerID).get(resourceRequested).getValue() : ((CatanParameters)gs.getGameParameters()).max_resources_request_trade;
                for (int i = 1; i <= nAvailableRequest; i++) {
                    actions.add(new DeepConstructNewOffer(player, stage, offeringPlayerID, otherPlayerID, resourceOffered, nOffered, resourceRequested, i));
                }
                break;
        }
        if (actions.size() == 0) {
            throw new AssertionError("DeepConstructNewOffer: Something went wrong in trade offer creation, at step: " + choice);
//            actions.add(new DoNothing());  // Cancel trade, something went wrong
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return offeringPlayerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        CatanGameState gs = ((CatanGameState) state);
        if (choice == ChooseNRequested) {
            // Complete
            gs.setTradeOffer(new OfferPlayerTrade(stage, resourceOffered, nOffered, resourceRequested, nRequested, offeringPlayerID, otherPlayerID));
        }

        if (action instanceof DoNothing) {
            // Trade was cancelled, something went wrong.
            choice = OfferComplete;
            gs.negotiationStepsCount = 0;
            gs.setTradeOffer(null);
            gs.setTurnOwner(offeringPlayerID);
            gs.nTradesThisTurn++;
        }
        else {
            if (choice == ChoosePlayerTradeWith) otherPlayerID = ((DeepConstructNewOffer) action).otherPlayerID;
            else if (choice == ChooseResourceOffer) resourceOffered = ((DeepConstructNewOffer) action).resourceOffered;
            else if (choice == ChooseNOffered) nOffered = ((DeepConstructNewOffer) action).nOffered;
            else if (choice == ChooseResourceRequested)
                resourceRequested = ((DeepConstructNewOffer) action).resourceRequested;
            else if (choice == ChooseNRequested) nRequested = ((DeepConstructNewOffer) action).nRequested;
            choice = Choice.values()[choice.ordinal() + 1];
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return choice == OfferComplete;
    }

    @Override
    public DeepConstructNewOffer copy() {
        return new DeepConstructNewOffer(player, stage, offeringPlayerID, otherPlayerID, resourceOffered, nOffered, resourceRequested, nRequested, execute, choice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepConstructNewOffer)) return false;
        DeepConstructNewOffer that = (DeepConstructNewOffer) o;
        return player == that.player && offeringPlayerID == that.offeringPlayerID && execute == that.execute && otherPlayerID == that.otherPlayerID && nOffered == that.nOffered && nRequested == that.nRequested && stage == that.stage && resourceOffered == that.resourceOffered && resourceRequested == that.resourceRequested && choice == that.choice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, offeringPlayerID, execute, stage, otherPlayerID, resourceOffered, nOffered, resourceRequested, nRequested, choice);
    }

    @Override
    public String toString() {
        if (choice == ChoosePlayerTradeWith) return String.format("(Deep 0) Trade p%d to ?", offeringPlayerID);
        else if (choice == ChooseResourceOffer) return String.format("(Deep 1) Trade p%d to p%d: X ? for Y ?", offeringPlayerID, otherPlayerID);
        else if (choice == ChooseNOffered) return String.format("(Deep 2) Trade p%d to p%d: X %s for Y ?", offeringPlayerID, otherPlayerID, resourceOffered);
        else if (choice == ChooseResourceRequested) return String.format("(Deep 3) Trade p%d to p%d: %d %s for Y ?", offeringPlayerID, otherPlayerID, nOffered, resourceOffered);
        else if (choice == ChooseNRequested) return String.format("(Deep 4) Trade p%d to p%d: %d %s for Y %s", offeringPlayerID, otherPlayerID, nOffered, resourceOffered, resourceRequested);
        return String.format("(Deep 5) Trade p%d to p%d: %d %s for %d %s", offeringPlayerID, otherPlayerID,
                nOffered, resourceOffered, nRequested, resourceRequested);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
