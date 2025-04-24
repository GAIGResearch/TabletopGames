package games.dominion.actions;

import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
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
    CardType cardType;

    public Vassal(int playerId) {
        super(CardType.VASSAL, playerId);
    }

    public Vassal(int playerId, boolean dummy) {
        super(CardType.VASSAL, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        Deck<DominionCard> drawPile = state.getDeck(DeckType.DRAW, player);
        Deck<DominionCard> discardPile = state.getDeck(DeckType.DISCARD, player);

        // Reshuffle if draw pile is empty
        if (drawPile.getSize() == 0) {
            discardPile.shuffle(state.getRnd());
            drawPile.add(discardPile);
            discardPile.clear();
        }

        // Discard top card from the deck
        DominionCard card = drawPile.draw();
        state.getDeck(DeckType.TABLE, player).add(card);
        cardType = card.cardType();

        return true;
    }

    @Override
    public Vassal copy() {
        Vassal retValue = new Vassal(player, dummyAction);
        retValue.executed = executed;
        retValue.cardType = cardType;
        return retValue;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        DominionGameState dgs = (DominionGameState) state;

        DominionCard card = dgs.getDeck(DeckType.TABLE, player).peek();

        assert card.cardType() == cardType: "Discarded CardType: " + card.cardType() + ", does not mach: " + cardType;

        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());

        // If the card discarded is an action card
        // it can be played
        if (card.isActionCard()) {
            DominionAction action = card.getAction(player);
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
        Deck<DominionCard> discardPile = dgs.getDeck(DeckType.DISCARD, player);
        executed = true;

        if (action instanceof DoNothing) {
            DominionCard playedCard = dgs.getDeck(DeckType.TABLE, player).draw();
            dgs.getDeck(DeckType.DISCARD, player).add(playedCard);
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
            return super.equals(obj) && other.executed == executed
                                     && other.cardType == cardType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int cardHash = cardType != null ? cardType.hashCode() : 9000;
        return super.hashCode() + (executed ? 1 : 0) + cardHash;
    }
    
}
