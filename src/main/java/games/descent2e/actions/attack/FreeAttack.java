package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;

import java.util.Objects;

import static games.descent2e.DescentHelper.hasLineOfSight;
import static games.descent2e.DescentHelper.inRange;

public class FreeAttack extends RangedAttack {

    protected boolean isMelee;
    public FreeAttack(int attackingFigure, int defendingFigure, boolean isMelee) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
        this.isFreeAttack = true;
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        attacker.setUsedExtraAction(true);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (f.getNActionsExecuted().isMaximum() && f.hasUsedExtraAction()) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);

        int range = MAX_RANGE;

        if (isMelee)
        {
            range = 1;
        }

        return hasLineOfSight(dgs, f.getPosition(), target.getPosition()) && inRange(f.getPosition(), target.getPosition(), range);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FreeAttack other) {
            if (other.isMelee == this.isMelee)
                return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isMelee);
    }


    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        if (isMelee)
        {
            return String.format("Free Melee Attack by " + attackerName + " on " + defenderName + "; " + result);
        }

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Free Ranged Attack by " + attackerName + " on " + defenderName + " (Range: " + distance + "); " + result);
    }

    @Override
    public String toString() {
        if (isMelee) return String.format("Free Melee Attack by %d on %d", attackingFigure, defendingFigure);
        return String.format("Free Ranged Attack by %d on %d", attackingFigure, defendingFigure);
    }

    public FreeAttack copy() {
        FreeAttack retValue = new FreeAttack(attackingFigure, defendingFigure, isMelee);
        copyComponentTo(retValue);
        return retValue;
    }
    public void copyComponentTo(FreeAttack target) {
        super.copyComponentTo(target);
        target.isMelee = isMelee;
    }
}
