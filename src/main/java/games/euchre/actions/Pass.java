package games.euchre.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.euchre.EuchreGameState;

/**
 * Decline to choose trumps.
 */
public class Pass extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ((EuchreGameState) gs).countPass();
        return true;  // the forward model passes the turn on
    }

    @Override
    public Pass copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pass;
    }

    @Override
    public int hashCode() {
        return 562301;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Pass";
    }
}
