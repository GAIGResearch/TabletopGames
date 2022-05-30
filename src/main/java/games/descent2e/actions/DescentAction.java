package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

import java.util.HashSet;

public abstract class DescentAction extends AbstractAction {

    HashSet<InterruptPoints> triggerPoints;

    public DescentAction(InterruptPoints triggerPoint) {
        this.triggerPoints = new HashSet<>();
        this.triggerPoints.add(triggerPoint);
    }
    public DescentAction(HashSet<InterruptPoints> triggerPoints) {
        this.triggerPoints = triggerPoints;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return execute((DescentGameState) gs);
    }
    public abstract boolean execute(DescentGameState gs);

}
