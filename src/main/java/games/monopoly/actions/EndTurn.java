package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class EndTurn extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public EndTurn copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndTurn;
    }

    @Override
    public int hashCode() {
        return 640213;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "End turn";
    }

    @Override
    public String toString() {
        return "EndTurn";
    }
}
