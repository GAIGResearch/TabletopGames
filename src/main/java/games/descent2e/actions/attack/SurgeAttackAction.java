package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

public class SurgeAttackAction extends DescentAction {

    public final Surge surge;

    public SurgeAttackAction(Surge surge) {
        super(Triggers.SURGE_DECISION);
        this.surge = surge;
    }

    @Override
    public String toString() {
        return surge.name();
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
        return true;
    }
}
