package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;

/**
 * This action calls for the Overpower Monster action to stop.
 * This is handled in the Overpower class.
 */

public class OverpowerStop extends DoNothing {
    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public AbstractAction copy() {
        return new OverpowerStop();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof OverpowerStop;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "End Overpower Movement";
    }
}
