package core.actions;

import core.AbstractGameState;
import core.components.Counter;

import java.util.Objects;

public class ModifyCounter extends AbstractAction {
    public int counterID;
    protected final int change;

    public ModifyCounter(int counterID, int change) {
        this.counterID = counterID;
        this.change = change;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Counter c = (Counter)gs.getComponentById(counterID);
        if (change > 0 && !c.isMaximum()) {
            c.increment(change);
            return true;
        } else if (change < 0 && !c.isMinimum()) {
            c.increment(change);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new ModifyCounter(counterID, change);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifyCounter)) return false;
        ModifyCounter that = (ModifyCounter) o;
        return counterID == that.counterID &&
                change == that.change;
    }

    @Override
    public int hashCode() {
        return Objects.hash(counterID, change);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Modify counter " + gameState.getComponentById(counterID).getComponentName() + " by " + change;
    }
}
