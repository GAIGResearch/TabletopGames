package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.DiscardSupporter;
import games.root.actions.PlaceSympathy;
import games.root.components.cards.RootCard;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpreadSympathy extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final int locationID;

    public enum Stage {
        Discard,
        Place,
    }

    Stage stage;
    int discardedCards = 0;
    int toDiscard;
    boolean executionComplete = false;

    public SpreadSympathy(int playerID, int locationID) {
        this.playerID = playerID;
        this.locationID = locationID;
        this.stage = Stage.Discard;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState currentState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (stage == Stage.Discard) {
            Deck<RootCard> supporters = currentState.getSupporters();
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            for (int i = 0; i < supporters.getSize(); i++) {
                if (supporters.get(i).suit == location.getClearingType() || supporters.get(i).suit == RootParameters.ClearingTypes.Bird) {
                    DiscardSupporter action = new DiscardSupporter(playerID, i, supporters.get(i).getComponentID(), false);
                    actions.add(action);
                }
            }
            return actions;
        } else if (stage == Stage.Place) {
            PlaceSympathy placeSympathy = new PlaceSympathy(playerID, locationID);
            actions.add(placeSympathy);
            return actions;
        }
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof DiscardSupporter) {
            discardedCards++;
            if (discardedCards == toDiscard) {
                stage = Stage.Place;
            }
        } else if (action instanceof PlaceSympathy) {
            executionComplete = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executionComplete;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        toDiscard = rp.sympathyDiscardCost.get(state.getSympathyTokens());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public SpreadSympathy copy() {
        SpreadSympathy other = new SpreadSympathy(playerID, locationID);
        other.toDiscard = toDiscard;
        other.discardedCards = discardedCards;
        other.stage = stage;
        return other;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SpreadSympathy that)) return false;
        return playerID == that.playerID && locationID == that.locationID && discardedCards == that.discardedCards && toDiscard == that.toDiscard && executionComplete == that.executionComplete && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, stage, discardedCards, toDiscard, executionComplete);
    }

    @Override
    public String toString() {
        return "p" + playerID + " spreads sympathy at location " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " spreads sympathy at location " + locationID;
    }
}
