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
    String heroName = "Tomble Burrowell";
    public ReturnToMapPlace() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        Move.replace(dgs, f);
        f.setFeatAvailable(false);
        f.setCanIgnoreEnemies(false);
        System.out.println("Tomble reappeared on the map!");
        return true;
    }

    @Override
    public ReturnToMapPlace copy() {
        return new ReturnToMapPlace();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        return  f.getName().contains(heroName) && f.isFeatAvailable() && !f.getNActionsExecuted().isMaximum() &&
                (f.getAttributeValue(Figure.Attribute.MovePoints) > 0 || f.hasMoved()) && f.isOffMap();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReturnToMapPlace;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Reappear";
    }
}