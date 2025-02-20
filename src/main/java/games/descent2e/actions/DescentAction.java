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
        this.triggerPoints = Set.of(triggerPoint);
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

    public Set<Triggers> getTriggers() {
        return triggerPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentAction that)) return false;
        return Objects.equals(triggerPoints, that.triggerPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggerPoints);
    }
}
