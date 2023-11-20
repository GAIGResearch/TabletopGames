package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;

public class DoubleAttack extends MultiAttack {

    // Widow Tarha's Heroic Feat
    public DoubleAttack(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);
        ((Hero) dgs.getActingFigure()).setFeatAvailable(false);
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return  !f.getNActionsExecuted().isMaximum() && super.defendingFigures.size() == 2;
    }

    public DoubleAttack copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        int attackingFigure = super.attackingFigure;
        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);

        Figure defender1 = (Figure) gameState.getComponentById(super.defendingFigures.get(0));
        Figure defender2 = (Figure) gameState.getComponentById(super.defendingFigures.get(1));

        String defenderName1 = defender1.getComponentName();
        String defenderName2 = defender2.getComponentName();

        String distance1 = Double.toString(getDistanceFromFigures(attacker, defender1));
        String distance2 = Double.toString(getDistanceFromFigures(attacker, defender2));

        return String.format("Heroic Feat: Double Attack by " + attackerName + " on " + defenderName1 + " (Range: " + distance1 + ") and " + defenderName2 + " (Range: " + distance2 + ")");
    }

    @Override
    public String toString() {
        return String.format("Heroic Feat: Double Attack both %d and %d", super.defendingFigures.get(0), super.defendingFigures.get(1));
    }

}
