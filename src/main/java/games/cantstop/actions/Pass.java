package games.cantstop.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class Pass extends AbstractAction {

    public final boolean bust;

    public Pass(boolean bust) {
        this.bust = bust;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // all the paperwork is done in the ForwardModel after this action is executed
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pass && ((Pass) obj).bust == bust;
    }

    @Override
    public int hashCode() {
        return 3927 + (bust ? 392 : 0);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Pass" + (bust ? " (bust)" : "");
    }
}
