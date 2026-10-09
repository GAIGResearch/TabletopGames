package games.schwimmen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class Pass extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        // the forward model counts the pass
        return true;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pass;
    }

    @Override
    public int hashCode() {
        return 734113;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "Pass";
    }
}
