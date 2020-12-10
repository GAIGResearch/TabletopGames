package games.diamant.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;

public class ExitFromCave extends AbstractAction implements IPrintable {
    @Override
    public boolean execute(AbstractGameState gs) {
        // Nothing to be executed. The actions are executed in the ForwardModel
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new ExitFromCave();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        return obj instanceof ExitFromCave;
    }

    // TODO: ???
    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Exit from cave";
    }
}
