package games.president.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.president.PresidentGameState;

public class Pass extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        PresidentGameState state = (PresidentGameState) gs;
        state.setPassesInRow(state.getPassesInRow() + 1);
        return true;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pass;
    }

    @Override
    public int hashCode() {
        return 610331;
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
