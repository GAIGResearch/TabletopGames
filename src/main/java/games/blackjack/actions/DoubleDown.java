package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;

/**
 * Double the bet on the hand being played, take exactly one more card, and finish the hand.
 */
public class DoubleDown extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        BlackjackGameState state = (BlackjackGameState) gs;
        int player = state.getCurrentPlayer();
        state.doubleBet(player, state.getActiveHand());
        state.getPlayerHand(player, state.getActiveHand()).add(state.drawCard());
        return true;  // the forward model finishes the hand
    }

    @Override
    public DoubleDown copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DoubleDown;
    }

    @Override
    public int hashCode() {
        return 730261;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Double down";
    }
}
