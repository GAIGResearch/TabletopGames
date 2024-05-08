package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.*;

public abstract class UseMonk extends AbstractAction {

    protected int actionPoints;

    public UseMonk(int actionPoints) {
        this.actionPoints = actionPoints;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        state.useAP(actionPoints);
        return _execute(state);
    }

    public abstract boolean _execute(DiceMonasteryGameState state);

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    public int getActionPoints() {
        return actionPoints;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

}
