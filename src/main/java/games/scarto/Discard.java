package games.scarto;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.TarotCard;

/**
 * In the dealer's exchange (ScartoParameters.dealerExchange), the dealer discards a card from their hand to the
 * scarto.
 */
public class Discard extends AbstractAction {

    public final TarotCard card;

    public Discard(TarotCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ScartoGameState state = (ScartoGameState) gs;
        state.getPlayerHand(state.getCurrentPlayer()).remove(card);  // throws if the player does not hold the card
        state.scarto.add(card);
        return true;
    }

    @Override
    public Discard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Discard that && card.equals(that.card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 591023;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Discard " + card;
    }
}
