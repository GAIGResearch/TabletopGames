package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;

public class GetFatiguedMovementPoints extends DescentAction {
    public GetFatiguedMovementPoints() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Spent 1 Stamina For 1 Movement Point";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.getActingFigure().getAttribute(Figure.Attribute.MovePoints).increment();
        gs.getActingFigure().getAttribute(Figure.Attribute.Fatigue).increment();
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        return !f.hasCondition(DescentTypes.DescentCondition.Immobilize) && !f.getAttribute(Figure.Attribute.Fatigue).isMaximum();
    }
}