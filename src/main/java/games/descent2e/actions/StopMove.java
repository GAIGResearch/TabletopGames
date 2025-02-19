package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.components.Figure;

public class StopMove extends DescentAction{

    final int f;
    public StopMove(int f) {
        super(Triggers.ANYTIME);
        this.f = f;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "End Movement action";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.f);
        f.setAttributeToMin(Figure.Attribute.MovePoints);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof StopMove sm && sm.f == this.f);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.f);
        return !f.getAttribute(Figure.Attribute.MovePoints).isMinimum() && !DescentHelper.canStillMove(f);
    }
}
