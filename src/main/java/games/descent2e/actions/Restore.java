package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Restore extends DescentAction{

    int restore;
    public Restore(int restore) {
        super(Triggers.ANYTIME);
        this.restore = restore;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Free Action: Restore +" + restore + " Health";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        f.incrementAttribute(Figure.Attribute.Health, restore);
        f.setUsedExtraAction(true);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new Restore(restore);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        return !f.hasUsedExtraAction() && !f.getAttribute(Figure.Attribute.Health).isMaximum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Restore restore1 = (Restore) o;
        return restore == restore1.restore;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), restore);
    }
}