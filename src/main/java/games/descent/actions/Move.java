package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

// TODO: perform a move action (can be interrupted for 2nd action, then continued) up to speed
public class Move extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Move();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Move;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Move";
    }
}
