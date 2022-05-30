package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.InterruptPoints;

public class AcolyteAction extends DescentAction {
    public AcolyteAction() {
        super(InterruptPoints.END_TURN);
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        return false;
    }
}
