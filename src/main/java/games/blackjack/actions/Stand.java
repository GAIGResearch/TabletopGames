package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * Take no more cards on the hand being played.
 */
public class Stand extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;  // the forward model moves on to the next hand
    }

    @Override
    public Stand copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stand;
    }

    @Override
    public int hashCode() {
        return 730237;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Stand";
    }
}
