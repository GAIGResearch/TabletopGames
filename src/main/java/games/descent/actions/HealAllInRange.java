package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class HealAllInRange extends AbstractAction {

    public HealAllInRange(String figureType, int amount, int range) {
        // TODO: heal all figures of given type in given range with given amount
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
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
}
