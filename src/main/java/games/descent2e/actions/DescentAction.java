package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

import java.util.HashSet;

public abstract class DescentAction extends AbstractAction {

    HashSet<Triggers> triggerPoints;

    public DescentAction(Triggers triggerPoint) {
        this.triggerPoints = new HashSet<>();
        this.triggerPoints.add(triggerPoint);
    }
    public DescentAction(HashSet<Triggers> triggerPoints) {
        this.triggerPoints = triggerPoints;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return execute((DescentGameState) gs);
    }
    public abstract boolean execute(DescentGameState gs);

}
