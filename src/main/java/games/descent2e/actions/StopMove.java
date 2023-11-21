package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

public class StopMove extends DescentAction{

    Figure f;
    public StopMove(Figure f) {
        super(Triggers.ANYTIME);
        this.f = f;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "End Movement action";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        f.setAttributeToMin(Figure.Attribute.MovePoints);
        return true;
    }

    @Override
    public DescentAction copy() {
        return new StopMove(f);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return !f.getAttribute(Figure.Attribute.MovePoints).isMinimum();
    }
}
