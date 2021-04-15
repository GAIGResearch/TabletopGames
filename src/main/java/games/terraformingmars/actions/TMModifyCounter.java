package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.Objects;

public class TMModifyCounter extends TMAction {
    public int counterID;
    public double change;

    public TMModifyCounter(int counterID, double change, boolean free) {
        super(-1, free);
        this.counterID = counterID;
        this.change = change;
    }

    public TMModifyCounter(TMTypes.ActionType actionType, int counterID, double change, boolean free) {
        super(actionType, -1, free);
        this.counterID = counterID;
        this.change = change;
    }

    public TMModifyCounter(TMTypes.StandardProject standardProject, int counterID, int change, boolean free) {
        super(standardProject, -1, free);
        this.counterID = counterID;
        this.change = change;
    }

    @Override
    public boolean _execute(TMGameState gs) {
        Counter c = (Counter)gs.getComponentById(counterID);
        if (change > 0 && !c.isMaximum()) {
            c.increment((int)change);
            return true;
        } else if (change < 0 && !c.isMinimum()) {
            c.increment((int)change);
            return true;
        }
        return false;
    }

    @Override
    public TMModifyCounter _copy() {
        return new TMModifyCounter(counterID, change, freeActionPoint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMModifyCounter)) return false;
        if (!super.equals(o)) return false;
        TMModifyCounter that = (TMModifyCounter) o;
        return counterID == that.counterID &&
                change == that.change;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), counterID, change);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Modify counter " + gameState.getComponentById(counterID).getComponentName() + " by " + change;
    }

    @Override
    public String toString() {
        return "Modify counter " + counterID + " by " + change;
    }
}