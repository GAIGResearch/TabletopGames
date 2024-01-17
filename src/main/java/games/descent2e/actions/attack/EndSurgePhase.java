package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

import java.util.Objects;

public class EndSurgePhase extends AbstractAction  {

    @Override
    public boolean execute(AbstractGameState gs) {
        // use up surges.
        ((MeleeAttack) Objects.requireNonNull(gs.currentActionInProgress())).surgesToSpend = 0;
        ((DescentGameState) gs).getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndSurgePhase;
    }

    @Override
    public int hashCode() {
        return 490341;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End Surge decision phase";
    }
}
