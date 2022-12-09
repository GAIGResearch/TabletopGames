package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class PassOnFavour extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        // does nothing
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PassOnFavour;
    }

    @Override
    public int hashCode() {
        return -3892795;
    }

    @Override
    public String toString() {
        return "Pass on using Favour";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
