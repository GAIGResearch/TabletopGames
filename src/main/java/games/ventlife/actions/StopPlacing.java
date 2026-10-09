package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * The current player places no more creatures this turn (after at least one Volcano Snail).
 */
public class StopPlacing extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        // PlaceMoreSnails ends when told
        return true;
    }

    @Override
    public StopPlacing copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StopPlacing;
    }

    @Override
    public int hashCode() {
        return 771309;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Place no more creatures";
    }
}
