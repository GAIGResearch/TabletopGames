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
 * Action to play the Vassal card. This card allows the player to draw the top card of their deck, and if it is an action card, they can choose to play it.
 */
public class Vassal extends DominionAction implements IExtendedSequence {

    boolean executed = false;
    boolean emptyDeck = false;
    DominionCard drawnCard;

    public Vassal(int playerId) {
        super(CardType.VASSAL, playerId);
    }

    public Vassal(int playerId, boolean dummy) {
        super(CardType.VASSAL, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public Vassal copy() {
        Vassal retValue = new Vassal(player, dummyAction);
        retValue.executed = executed;
        retValue.emptyDeck = emptyDeck;
        retValue.drawnCard = drawnCard;
        return retValue;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        DominionGameState dgs = (DominionGameState) state;

        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());

        if (dgs.getDeck(DeckType.DRAW, player).getSize() == 0) {
            emptyDeck = true;
            return actions;
        }

        // Draw the top card of the deck, and if action card, it can be played
        // This card is discarded afterwards
        drawnCard = dgs.getDeck(DeckType.DRAW, player).draw().copy();
        if (drawnCard.isActionCard()) {
            DominionAction action = drawnCard.getAction(player);
            action.dummyAction = true;
            actions.add(action);
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
        executed = true;

        if (emptyDeck) {
            return;
        }
        else if (action instanceof DoNothing) {
            dgs.getDeck(DeckType.DISCARD, player).add(drawnCard);
        }
        else {
            dgs.getDeck(DeckType.TABLE, player).add(drawnCard);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vassal) {
            Vassal other = (Vassal) obj;

            boolean sameCard;

            if (drawnCard == null && other.drawnCard == null) {
                sameCard = true;
            }
            else if (drawnCard != null && drawnCard.equals(other.drawnCard)) {
                sameCard = true;
            }
            else {
                sameCard = false;
            }

            return super.equals(obj) && other.executed == executed
                                     && emptyDeck == other.emptyDeck
                                     && sameCard;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int cardHash = drawnCard != null ? drawnCard.hashCode() : 0;
        return super.hashCode() + (executed ? 1 : 0) + cardHash + (emptyDeck ? 1 : 0);
    }
    
}
