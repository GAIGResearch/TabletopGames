package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;

public class AttackAllAdjacent extends MultiAttack {

    public AttackAllAdjacent(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        // TODO: current acting figure is doing this, find it
        // TODO: find all adjacent enemies next to the figure and attack them as in super class
        dgs.setActionInProgress(this);
        if (dgs.getActingFigure() instanceof Hero) {((Hero) dgs.getActingFigure()).setFeatAvailable(false); }
        super.execute(dgs);
        dgs.setActionInProgress(null);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        // TODO: Can only use if wielding a Magic weapon
        return !f.getNActionsExecuted().isMaximum();
    }

    public AttackAllAdjacent copy() {
        AttackAllAdjacent retValue = new AttackAllAdjacent(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(AttackAllAdjacent target) {
        super.copyComponentTo(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Attack all adjacent monsters";
    }

    @Override
    public String toString() {
        return "Heroic Feat: Attack all adjacent monsters";
    }

}
