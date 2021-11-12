package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class DebugAction extends AbstractAction {

    boolean debug = false;

    @Override
    public boolean execute(AbstractGameState gs) {
        if (debug)  System.out.println("Action played!");
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
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
