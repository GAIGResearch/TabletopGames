package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.catan.actions.trade.DeepCounterOffer.Choice.*;

public class DeepCounterOffer extends AbstractAction implements IExtendedSequence {
    public enum Choice {
        ChooseNOffered,
        ChooseNRequested,
        CounterOfferComplete
    }

    public final int player;
    public final boolean execute;
    public final OfferPlayerTrade.Stage stage;

    public int nOffered;
    public int nRequested;
    Choice choice;

    // 1. Create action. Choose n resources to be offered next
    public DeepCounterOffer(OfferPlayerTrade.Stage stage, int player) {
        this.player = player;
        this.execute = true;
        this.choice = ChooseNOffered;
        this.stage = stage;
    }

    // 2. Choose n resources to be requested next
    public DeepCounterOffer(OfferPlayerTrade.Stage stage, int player, int nOffered) {
        this.player = player;
        this.nOffered = nOffered;
        this.execute = false;
        this.choice = ChooseNRequested;
        this.stage = stage;
    }
    // 3. Complete
    public DeepCounterOffer(OfferPlayerTrade.Stage stage, int player, int nOffered, int nRequested) {
        this.player = player;
        this.nOffered = nOffered;
        this.nRequested = nRequested;
        this.execute = false;
        this.choice = Choice.CounterOfferComplete;
        this.stage = stage;
    }
    // Copy constructor
    public DeepCounterOffer(OfferPlayerTrade.Stage stage, int player, int nOffered, int nRequested, boolean execute, Choice choice) {
        this.player = player;
        this.nOffered = nOffered;
        this.nRequested = nRequested;
        this.execute = execute;
        this.choice = choice;
        this.stage = stage;
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
        OfferPlayerTrade opt = (OfferPlayerTrade) gs.getTradeOffer();
        int nAvailableOffer = opt.offeringPlayerID == player? gs.getPlayerResources(player).get(opt.resourceOffered).getValue() : ((CatanParameters)gs.getGameParameters()).max_resources_request_trade;
        int nAvailableRequest = opt.otherPlayerID == player? gs.getPlayerResources(player).get(opt.resourceRequested).getValue() : ((CatanParameters)gs.getGameParameters()).max_resources_request_trade;

        // Fill in the respective choice
        switch(choice) {
            case ChooseNOffered:
                for (int i = 1; i <= nAvailableOffer; i++) {
                    // Only allow to be same as old if there exists a different option for the other parameter
                    if (i != opt.nOffered || nAvailableRequest >= 2) {
                        actions.add(new DeepCounterOffer(stage, player, i));
                    }
                }
                break;
            case ChooseNRequested:
                for (int i = 1; i <= nAvailableRequest; i++) {
                    // Only allow variation where at least one difference to old offer is present
                    if (i != opt.nRequested || nOffered != opt.nOffered) {
                        actions.add(new DeepCounterOffer(stage, player, nOffered, i));
                    }
                }
                break;
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (choice == Choice.ChooseNRequested) {
            // Complete
            OfferPlayerTrade opt = (OfferPlayerTrade) ((CatanGameState)state).getTradeOffer();
            ((CatanGameState)state).setTradeOffer(new OfferPlayerTrade(stage, opt.resourceOffered, nOffered,
                    opt.resourceRequested, nRequested, opt.offeringPlayerID, opt.otherPlayerID));
        }

        if (choice == ChooseNOffered) nOffered = ((DeepCounterOffer)action).nOffered;
        else if (choice == ChooseNRequested) nRequested = ((DeepCounterOffer)action).nRequested;
        choice = Choice.values()[choice.ordinal()+1];
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return choice == CounterOfferComplete;
    }

    @Override
    public DeepCounterOffer copy() {
        return new DeepCounterOffer(stage, player, nOffered, nRequested, execute, choice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepCounterOffer)) return false;
        DeepCounterOffer that = (DeepCounterOffer) o;
        return player == that.player && execute == that.execute && nOffered == that.nOffered && nRequested == that.nRequested && stage == that.stage && choice == that.choice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, execute, stage, nOffered, nRequested, choice);
    }

    @Override
    public String toString() {
        if (choice == ChooseNOffered) return String.format("(Deep 0) p%d counter-offers trade", player);
        else if (choice == ChooseNRequested) return String.format("(Deep 1) p%d counter-offers trade: %d resources to be offered.", player, nOffered);
        return String.format("(Deep 2) p%d counter-offers trade: %d for %d", player, nOffered, nRequested);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        OfferPlayerTrade opt = (OfferPlayerTrade) ((CatanGameState)gameState).getTradeOffer();
        if (choice == ChooseNOffered) return String.format("(Deep 0) Counter-offer (p%d): " + opt.toString(), player);
        else if (choice == ChooseNRequested) return String.format("(Deep 1) Trade counter-offer (p%d): p%d to p%d : %d (old: %d) %s for ? (old: %d) %s",
                player, opt.offeringPlayerID, opt.otherPlayerID, nOffered, opt.nOffered, opt.resourceOffered, opt.nRequested, opt.resourceRequested);
        return String.format("(Deep 2) Trade counter-offer (p%d): p%d to p%d : %d (old: %d) %s for %d (old: %d) %s",
                player, opt.offeringPlayerID, opt.otherPlayerID, nOffered, opt.nOffered, opt.resourceOffered, nRequested, opt.nRequested, opt.resourceRequested);
    }
}
