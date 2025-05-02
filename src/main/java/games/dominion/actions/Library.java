package games.dominion.actions;

import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.DominionConstants.DeckType;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

/**
 * Action to play the Library card. This card allows the player to draw until they have 7 cards in hand, allowing them to discard any action cards drawn in the process.
 */
public class Library extends DominionAction implements IExtendedSequence {

    boolean executed = false;

    public Library(int playerId) {
        super(CardType.LIBRARY, playerId);
    }

    public Library(int playerId, boolean dummy) {
        super(CardType.LIBRARY, playerId, dummy);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        DominionGameState dgs = (DominionGameState) state;
        List<AbstractAction> actions = new ArrayList<>();

        boolean couldDraw = dgs.drawCard(player);
        actions.add(new DoNothing());

        // Edge case if they have no hands left in play (discard and draw pile is empty)
        if (!couldDraw) {
            return actions;
        }

        DominionCard drawnCard = dgs.getDeck(DeckType.HAND,player).peek();

        // If it is an action card they have the option to discard it
        if (drawnCard.isActionCard()) {
            actions.add(new MoveCard(drawnCard.cardType(), player, DeckType.HAND, player, DeckType.DISCARD, true));
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        DominionGameState dgs = (DominionGameState) state;

        // If the player has 7 cards in hand, the action is complete
        if (dgs.getDeck(DeckType.HAND, player).getSize() >= 7) {
            executed = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public Library copy() {
        Library retValue = new Library(player, dummyAction);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Library other) {
            return super.equals(obj) && other.executed == executed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (executed ? 1: 0);
    }
    
}
