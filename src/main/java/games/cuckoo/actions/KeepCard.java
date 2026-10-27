package games.cuckoo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * Keep the card held.
 */
public class KeepCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;  // the forward model passes the turn on
    }

    @Override
    public KeepCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeepCard;
    }

    @Override
    public int hashCode() {
        return 481903;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Keep card";
    }
}
