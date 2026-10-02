package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;

/**
 * Bet the given number of chips on the coming hand.
 */
public class Bet extends AbstractAction {

    public final int amount;

    public Bet(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BlackjackGameState state = (BlackjackGameState) gs;
        state.placeBet(state.getCurrentPlayer(), amount);
        return true;
    }

    @Override
    public Bet copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bet that && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return amount + 730211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Bet " + amount;
    }
}
