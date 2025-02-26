package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

public class RemoveFromMap extends DescentAction {

    // Tomble Burrowell Heroic Feat
    // Part 1 of 3
    public RemoveFromMap() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        Move.remove(dgs, f);
        f.setOffMap(true);

        //System.out.println("Tomble vanished from the map!");

        // TODO: Need to clarify rules regarding if Tomble can act after using his Heroic Feat
        // But by the wording of the rules, it seems like he ends turn immediately after using it
        // As Hero Tokens cannot interact with other figures
        f.setAttributeToMin(Figure.Attribute.MovePoints);
        f.getNActionsExecuted().setToMax();
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public RemoveFromMap copy() {
        return new RemoveFromMap();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return !f.getNActionsExecuted().isMaximum() && !f.isOffMap();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RemoveFromMap && super.equals(obj);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Vanish, ending your turn, then next turn reappear up to 4 spaces away.";
    }
    @Override
    public String toString() {
        return "Heroic Feat: Vanish (1/3)";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 112011;
    }
}
