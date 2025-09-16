package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.herofeats.AttackAllAdjacent;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;

import static games.descent2e.DescentHelper.checkAdjacent;
import static games.descent2e.DescentHelper.getAttackType;

public class Whirlwind extends AttackAllAdjacent {

    public Whirlwind(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
        this.minRange = 0;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // No point in using Whirlwind if we'd only hit one target - may as well use a regular attack instead
        if (defendingFigures.size() < 2) return false;
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum() || f.getNActionsExecuted().isMaximum()) return false;
        DescentTypes.AttackType attackType = getAttackType(f);
        if (attackType != DescentTypes.AttackType.MELEE && attackType != DescentTypes.AttackType.BOTH) return false;

        for (int defendingFigure : defendingFigures)
        {
            // We can only hit adjacent figures, none of our targets should be more than 1 space away
            // This also means we can ignore the Air Immunity passive, as it only applies to non-adjacent attacks
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;
            if(!checkAdjacent(dgs, f, target)) return false;
        }

        return true;
    }

    public Whirlwind copy() {
        Whirlwind retValue = new Whirlwind(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(Whirlwind target) {
        super.copyComponentTo(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString() + result;
    }

    @Override
    public String toString() {
        return "Whirlwind: Attack all adjacent monsters";
    }
}
