package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

public class ReturnToMapPlace extends DescentAction {

    // Tomble Burrowell Heroic Feat
    // Part 3 of 3
    public ReturnToMapPlace() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        Move.replace(dgs, f);
        if (f instanceof Hero) {((Hero) f).setFeatAvailable(false);}
        f.setCanIgnoreEnemies(false);
        //System.out.println("Tomble reappeared on the map!");
        f.addActionTaken(toString());
        f.setHasMoved(false);
        return true;
    }

    @Override
    public ReturnToMapPlace copy() {
        return new ReturnToMapPlace();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return  !f.getNActionsExecuted().isMaximum() && (f.getAttributeValue(Figure.Attribute.MovePoints) > 0 || f.hasMoved()) && f.isOffMap();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReturnToMapPlace && super.equals(obj);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Reappear";
    }

    @Override
    public String toString() {
        return "Heroic Feat: Reappear (3/3)";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 112003;
    }
}
