package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

public class EndCurrentPhase extends AbstractAction {

    MeleeAttack currentAction;

    @Override
    public boolean execute(AbstractGameState gs) {
        ((MeleeAttack) gs.currentActionInProgress()).skip = true;
        System.out.println("Skipping current phase");
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndCurrentPhase;
    }

    @Override
    public int hashCode() {
        return 490404;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End current decision phase";
    }
}
