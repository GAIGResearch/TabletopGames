package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

public class Rest extends DescentAction{
    public Rest() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Rest";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero)gs.getActingFigure();
        hero.setRested(true);
        hero.getNActionsExecuted().increment();
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        return f instanceof Hero && f.getAttributeValue(Figure.Attribute.Fatigue) > 0 && !f.getNActionsExecuted().isMaximum() && !(((Hero) f).hasRested());
    }

    @Override
    public int hashCode() {
        return 111777;
    }
}
