package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;

public class GetMovementPoints extends DescentAction {
    public GetMovementPoints() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Get movement points";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.getActingFigure().setAttributeToMax(Figure.Attribute.MovePoints);
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        return !f.hasCondition(DescentTypes.DescentCondition.Immobilize) && !f.getNActionsExecuted().isMaximum();
    }
}
