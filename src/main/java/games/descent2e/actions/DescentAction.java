package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class DescentAction extends AbstractAction {

    final Set<Triggers> triggerPoints;

    public DescentAction(Triggers triggerPoint) {
        this.triggerPoints = new HashSet<>();
        this.triggerPoints.add(triggerPoint);
    }
    public DescentAction(Set<Triggers> triggerPoints) {
        this.triggerPoints = triggerPoints;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return execute((DescentGameState) gs);
    }
    public abstract boolean execute(DescentGameState gs);
    public abstract DescentAction copy();
    public boolean canExecute(Triggers currentPoint, DescentGameState dgs) {
        if (triggerPoints.contains(currentPoint)) return canExecute(dgs);
        return false;
    }
    public abstract boolean canExecute(DescentGameState dgs);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentAction)) return false;
        DescentAction that = (DescentAction) o;
        return Objects.equals(triggerPoints, that.triggerPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggerPoints);
    }
}
