package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * Pass the turn. Only available when no card can be played and none can be drawn.
 */
public class Pass extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        // Nothing changes: the forward model ends the turn
        return true;
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
        return 382902;
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
