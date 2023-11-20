package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

public class ReturnToMapMove extends DescentAction {

    // Tomble Burrowell Heroic Feat
    // Part 2 of 3
    int distance;
    public ReturnToMapMove(int distance) {
        super(Triggers.ACTION_POINT_SPEND);
        this.distance = distance;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        f.setAttribute(Figure.Attribute.MovePoints, distance);
        f.setCanIgnoreEnemies(true);
        //System.out.println("Tomble is choosing where to reappear!");
        return true;
    }

    @Override
    public ReturnToMapMove copy() {
        return new ReturnToMapMove(distance);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return  !f.getNActionsExecuted().isMaximum() && f.getAttributeValue(Figure.Attribute.MovePoints) == 0 && !f.hasMoved() && f.isOffMap();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReturnToMapMove)
            return ((ReturnToMapMove) obj).distance == distance;
        return false;
    }

    public int hashCode() {
        return Objects.hash(distance);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Move up to 4 spaces away before reappearing";
    }
    @Override
    public String toString() {
        return "Heroic Feat: Tomble Burrowell - Move (2/3)";
    }
}
