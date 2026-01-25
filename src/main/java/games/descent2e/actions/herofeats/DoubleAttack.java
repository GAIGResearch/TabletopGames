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
        ((Hero) dgs.getActingFigure()).setFeatAvailable(false);
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f instanceof Hero && !((Hero) f).isFeatAvailable()) return false;
        return  super.canExecute(dgs) && super.defendingFigures.size() == 2;
    }

    public DoubleAttack copy() {
        DoubleAttack retValue = new DoubleAttack(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(DoubleAttack target) {
        super.copyComponentTo(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        int attackingFigure = super.attackingFigure;
        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);

        Figure defender1 = (Figure) gameState.getComponentById(super.defendingFigures.get(0));
        Figure defender2 = (Figure) gameState.getComponentById(super.defendingFigures.get(1));

        String defenderName1 = defender1.getComponentName();
        String defenderName2 = defender2.getComponentName();
        String attackerName = attacker.getComponentName().replace("Hero: ", "");

        String distance1 = Double.toString(getDistanceFromFigures(attacker, defender1));
        String distance2 = Double.toString(getDistanceFromFigures(attacker, defender2));

        return String.format("Heroic Feat: Double Attack by " + attackerName + " on " + defenderName1 + " (Range: " + distance1 + ") and " + defenderName2 + " (Range: " + distance2 + "); " + result);
    }

    @Override
    public String toString() {
        return String.format("Heroic Feat: Double Attack by %d on both %d and %d", super.attackingFigure, super.defendingFigures.get(0), super.defendingFigures.get(1));
    }

}
