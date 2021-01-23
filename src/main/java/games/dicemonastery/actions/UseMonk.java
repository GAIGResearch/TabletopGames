package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.*;

public class UseMonk extends AbstractAction {

    int monkId;
    ActionArea area;

    public UseMonk(int monkId, ActionArea area) {
        this.monkId = monkId;
        this.area = area;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        state.moveMonk(monkId, area, ActionArea.DORMITORY);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UseMonk;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Sow Wheat";
    }
}
