package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

public class HealAllInRange extends DescentAction {

    public HealAllInRange(String figureType, int amount, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        // TODO: heal all figures of given type in given range with given amount
    }

    @Override
    public boolean execute(DescentGameState gs) {
        return false;
    }

    @Override
    public HealAllInRange copy() {
        return null;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
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
