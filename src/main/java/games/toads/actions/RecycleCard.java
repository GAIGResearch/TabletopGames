package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

public class RecycleCard extends AbstractAction {

    public final ToadCard discardedCard;

    public RecycleCard(ToadCard discardedCard) {
        this.discardedCard = discardedCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        if (discardedCard == null)
            return true;
        // i.e. we discard nothing
        int player = state.getCurrentPlayer();
        state.getPlayerHand(player).remove(discardedCard);
        // we move the card to the bottom of the Deck, and mark it as known to us
        PartialObservableDeck<ToadCard> playerDeck = state.getPlayerDeck(player);
        playerDeck.addToBottom(discardedCard);
        playerDeck.setVisibilityOfComponent(playerDeck.getSize() - 1, player, true);

        state.getPlayerHand(player).add(playerDeck.draw()); // draw a new card
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecycleCard rc) {
            if (discardedCard == null)
                return rc.discardedCard == null;
            return discardedCard.equals(rc.discardedCard);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (discardedCard == null ? 0 : discardedCard.hashCode()) + 91;
    }

    @Override
    public String toString() {
        if (discardedCard == null)
            return "Recycle nothing";
        return "Recycle " + discardedCard;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
