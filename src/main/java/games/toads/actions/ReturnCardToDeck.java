package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

/**
 * At the start of a War, the current player puts a card from their hand on the bottom of their deck, where only they
 * know it is.
 */
public class ReturnCardToDeck extends AbstractAction {

    public final ToadCard card;

    public ReturnCardToDeck(ToadCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        int player = state.getCurrentPlayer();
        state.getPlayerHand(player).remove(card);
        PartialObservableDeck<ToadCard> playerDeck = state.getPlayerDeck(player);
        playerDeck.addToBottom(card);
        playerDeck.setVisibilityOfComponent(playerDeck.getSize() - 1, player, true);
        return true;
    }

    @Override
    public ReturnCardToDeck copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReturnCardToDeck other && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 574241;
    }

    @Override
    public String toString() {
        return "Return " + card + " to the bottom of the deck";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
