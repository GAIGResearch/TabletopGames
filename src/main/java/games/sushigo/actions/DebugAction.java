package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class DebugAction extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        System.out.println("Action played!");
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof DebugAction;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "This is a debug action.";
    }
}
