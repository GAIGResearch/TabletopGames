package games.skitgubbe.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.skitgubbe.SkitgubbeGameState;

/**
 * Phase two: the current player takes the top card of the trick into their collected cards.
 */
public class PickUp extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        SkitgubbeGameState state = (SkitgubbeGameState) gs;
        state.getCollectedCards(state.getCurrentPlayer()).add(state.getTrick().draw());
        return true;
    }

    @Override
    public PickUp copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PickUp;
    }

    @Override
    public int hashCode() {
        return 518827;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Pick up";
    }
}
