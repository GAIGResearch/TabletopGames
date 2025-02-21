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
    public String toString() { return "Rest"; }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero)gs.getActingFigure();
        hero.setRested(true);
        hero.getNActionsExecuted().increment();
        hero.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // Control limit for fatigue, to stop agents from spamming Rest when they only have 1 fatigue
        int control = 2;
        return f instanceof Hero && !f.getNActionsExecuted().isMaximum() &&
                (f.getAttributeValue(Figure.Attribute.Fatigue) > control || f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) &&
                !(((Hero) f).hasRested());
    }

    @Override
    public int hashCode() {
        return 111777;
    }
}
