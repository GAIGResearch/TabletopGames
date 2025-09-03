package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.archetypeskills.PrayerOfPeace;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class FreeAttack extends RangedAttack {

    protected boolean isMelee;
    public FreeAttack(int attackingFigure, int defendingFigure, boolean isMelee, boolean reach) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
        this.isFreeAttack = true;
        this.hasReach = reach;
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

        if (!PrayerOfPeace.canAttackPrayer(dgs, f)) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        return checkAllSpaces(dgs, f, target, getRange(), true);
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
        FreeAttack retValue = new FreeAttack(attackingFigure, defendingFigure, isMelee, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }
    public void copyComponentTo(FreeAttack target) {
        super.copyComponentTo(target);
        target.isMelee = isMelee;
    }
}
