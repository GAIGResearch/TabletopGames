package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;

/**
 * Buy insurance against a dealer Blackjack (half the bet, paid 2:1 if the dealer has Blackjack), or decline it.
 */
public class Insurance extends AbstractAction {

    public final boolean buy;

    public Insurance(boolean buy) {
        this.buy = buy;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (buy) {
            BlackjackGameState state = (BlackjackGameState) gs;
            state.buyInsurance(state.getCurrentPlayer());
        }
        return true;
    }

    @Override
    public Insurance copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Insurance that && buy == that.buy;
    }

    @Override
    public int hashCode() {
        return buy ? 730249 : 730253;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return buy ? "Buy insurance" : "Decline insurance";
    }
}
