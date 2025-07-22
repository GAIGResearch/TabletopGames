package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.actions.herofeats.AttackAllAdjacent;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;

import static games.descent2e.DescentHelper.checkAdjacent;
import static games.descent2e.DescentHelper.inRange;

public class Fire extends AttackAllAdjacent {
    public Fire(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (defendingFigures.size() < 1) return false;
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;
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

    public Fire copy() {
        Fire retValue = new Fire(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(Fire target) {
        super.copyComponentTo(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Fire: Attack all adjacent figures" + result;
    }

    @Override
    public String toString() {
        return "Fire: Attack all adjacent figures";
    }
}
