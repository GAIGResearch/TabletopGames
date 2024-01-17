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
        return toString();
    }
    @Override
    public String toString() { return "Get Movement Points"; }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.getActingFigure().setAttributeToMax(Figure.Attribute.MovePoints);
        gs.getActingFigure().getNActionsExecuted().increment();
        gs.getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public int hashCode() {
        return 111500;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();

        return !f.hasCondition(DescentTypes.DescentCondition.Immobilize) && !f.getNActionsExecuted().isMaximum() && f.getAttribute(Figure.Attribute.MovePoints).isMinimum();
    }
}
