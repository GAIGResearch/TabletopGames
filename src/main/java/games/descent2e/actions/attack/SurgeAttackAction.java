package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

import java.util.Objects;

public class SurgeAttackAction extends DescentAction {

    public final Surge surge;
    public final int figureSource;

    public SurgeAttackAction(Surge surge, int figure) {
        super(Triggers.SURGE_DECISION);
        this.surge = surge;
        this.figureSource = figure;
    }

    @Override
    public String toString() {
        return surge.name() + " : " + figureSource;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean execute(DescentGameState gs) {
        MeleeAttack attack = (MeleeAttack) gs.currentActionInProgress();
        surge.apply(attack, gs);
        return true;
    }

    @Override
    public DescentAction copy() {
        return this; // immutable
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return dgs.getActingFigure().getComponentID() == figureSource;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SurgeAttackAction) {
            SurgeAttackAction o = (SurgeAttackAction) other;
            return o.figureSource == figureSource && o.surge == surge;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(figureSource, surge.ordinal()) - 492209;
    }
}
