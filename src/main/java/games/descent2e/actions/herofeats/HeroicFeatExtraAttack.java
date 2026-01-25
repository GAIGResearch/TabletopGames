package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

import static games.descent2e.DescentHelper.hasLineOfSight;
import static games.descent2e.DescentHelper.inRange;

public class HeroicFeatExtraAttack extends FreeAttack {

    // Grisban the Thirsty's Heroic Feat

    public HeroicFeatExtraAttack(int attackingFigure, int defendingFigure, boolean isMelee) {
        super(attackingFigure, defendingFigure, isMelee);
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        Figure f = state.getActingFigure();
        boolean hasExtraAction = f.hasUsedExtraAction();
        f.setUsedExtraAction(false);
        if (f instanceof Hero) {((Hero) f).setFeatAvailable(false);}
        super.execute(state);

        // Restore the extra action if it was previously available
        // This is not considered an extra action (a Heroic Feat action), so it should not interfere with the extra action
        f.setUsedExtraAction(hasExtraAction);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (!(f instanceof Hero) || ((Hero) f).isFeatAvailable()) {
            Figure target = (Figure) dgs.getComponentById(defendingFigure);

            int range = MAX_RANGE;

            if (isMelee) {
                range = 1;
            }

            return hasLineOfSight(dgs, f.getPosition(), target.getPosition()) && inRange(f.getPosition(), target.getPosition(), range);
        }
        return false;
    }

    public HeroicFeatExtraAttack copy() {
        HeroicFeatExtraAttack retValue = new HeroicFeatExtraAttack(attackingFigure, defendingFigure, isMelee);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(FreeAttack target) {
        super.copyComponentTo(target);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HeroicFeatExtraAttack) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 29;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        if (isMelee)
        {
            return String.format("Heroic Feat: Extra Melee Attack by " + attackerName + " on " + defenderName + "; " + result);
        }

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Heroic Feat: Extra Ranged Attack by " + attackerName + " on " + defenderName + " (Range: " + distance + "); " + result);
    }

    @Override
    public String toString() {
        if (isMelee) return String.format("Heroic Feat: Free Melee Attack by %d on %d", attackingFigure, defendingFigure);
        return String.format("Heroic Feat: Free Ranged Attack by %d on %d", attackingFigure, defendingFigure);
    }

}
