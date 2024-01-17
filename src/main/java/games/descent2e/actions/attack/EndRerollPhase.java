package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

public class EndRerollPhase extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ((DescentGameState) gs).getActingFigure().setRerolled(true);
        ((DescentGameState) gs).getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndRerollPhase;
    }

    @Override
    public int hashCode() {
        return 490342;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End Reroll decision phase";
    }
}
