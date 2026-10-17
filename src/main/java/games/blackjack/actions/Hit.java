package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;

/**
 * Take the top card of the draw deck onto the hand being played.
 */
public class Hit extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        BlackjackGameState state = (BlackjackGameState) gs;
        state.getPlayerHand(state.getCurrentPlayer(), state.getActiveHand()).add(state.drawCard());
        return true;
    }

    @Override
    public Hit copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hit;
    }

    @Override
    public int hashCode() {
        return 730223;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Hit";
    }
}
