package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpreadSympathy extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public RootBoardNodeWithRootEdges location;

    public enum Stage {
        Discard,
        Place,
    }

    public Stage stage;
    public int discardedCards = 0;
    public int toDiscard;
    public boolean executionComplete = false;

    public SpreadSympathy(int playerID, RootBoardNodeWithRootEdges location) {
        this.playerID = playerID;
        this.location = location;
        this.stage = Stage.Discard;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState currentState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (stage == Stage.Discard) {
            Deck<RootCard> supporters = currentState.getSupporters();
            for (int i = 0; i < supporters.getSize(); i++) {
                if (supporters.get(i).suit == location.getClearingType() || supporters.get(i).suit == RootParameters.ClearingTypes.Bird) {
                    DiscardSupporter action = new DiscardSupporter(playerID, supporters.get(i), false);
                    actions.add(action);
                }
            }
            return actions;
        } else if (stage == Stage.Place) {
            PlaceSympathy placeSympathy = new PlaceSympathy(playerID, location);
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
        toDiscard = rp.SympathyDiscardCost.get(state.getSympathyTokens());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public SpreadSympathy copy() {
        SpreadSympathy other = new SpreadSympathy(playerID, location);
        other.toDiscard = toDiscard;
        other.discardedCards = discardedCards;
        other.stage = stage;
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof SpreadSympathy) {
            SpreadSympathy other = (SpreadSympathy) obj;
            return playerID == other.playerID && location.getComponentID()==other.location.getComponentID();

        } else return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, location);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " spreads sympathy at location " + location.identifier;
    }
}
