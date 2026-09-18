package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

/**
 * Draw the top card of the stock into the hand. Only available when no card can be played; ends the turn.
 * If the stock is empty, the discards under the top card are first shuffled to form a new stock.
 */
public class DrawCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        CZEGameState state = (CZEGameState) gs;
        Deck<FrenchCard> stock = state.getDrawDeck();
        if (stock.getSize() == 0) {
            // Shuffle every discard except the top card to form a new stock; the suit to match is unchanged
            Deck<FrenchCard> discards = state.getDiscardPile();
            FrenchCard top = discards.draw();
            while (discards.getSize() > 0)
                stock.add(discards.draw());
            discards.add(top);
            stock.shuffle(state.getRnd());
        }
        state.getPlayerHands().get(state.getCurrentPlayer()).add(stock.draw());
        return true;
    }

    @Override
    public DrawCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DrawCard;
    }

    @Override
    public int hashCode() {
        return 382901;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Draw a card";
    }
}
