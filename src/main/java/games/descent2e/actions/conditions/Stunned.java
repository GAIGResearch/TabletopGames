package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.Figure;

import static games.descent2e.actions.Triggers.*;

public class Stunned extends DescentAction {

    public Stunned() {
        super(ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Remove Stunned";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.getActingFigure().removeCondition(DescentTypes.DescentCondition.Stun);
        dgs.getActingFigure().getNActionsExecuted().increment();
        dgs.getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        return f.hasCondition(DescentTypes.DescentCondition.Stun) && !f.getNActionsExecuted().isMaximum();
    }

    public int hashCode() {
        return 55555;
    }
}
