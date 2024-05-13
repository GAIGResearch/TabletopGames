package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class FlankCardPlayed extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        throw new AssertionError("Not playable - this is a placeholder action to show that we do not know what card they played");
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FlankCardPlayed;
    }

    @Override
    public int hashCode() {
        return -250783;
    }

    @Override
    public String toString() {
        return "Unknown Flank card played";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
