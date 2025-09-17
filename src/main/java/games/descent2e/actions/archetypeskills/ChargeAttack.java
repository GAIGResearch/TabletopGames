package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.components.Figure;

import java.util.Objects;

import static games.descent2e.DescentHelper.checkAllSpaces;

public class ChargeAttack extends FreeAttack {

    public ChargeAttack(int attackingFigure, int defendingFigure, boolean reach) {
        super(attackingFigure, defendingFigure, true, reach);
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        boolean oldExtra = attacker.hasUsedExtraAction();
        attacker.setUsedExtraAction(false);
        super.execute(state);
        attacker.setUsedExtraAction(oldExtra);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        if (!dgs.isActionInProgress()) return false;
        if (!(dgs.currentActionInProgress() instanceof Charge charge)) return false;
        if (!charge.hasMoved()) return false;

        Figure f = dgs.getActingFigure();
        if (f == null) return false;

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
        if (obj instanceof ChargeAttack other) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Free Melee Attack", "Charge Attack");}

    @Override
    public String toString() {
        return super.toString().replace("Free Melee Attack", "Charge Attack");
    }

    public ChargeAttack copy() {
        ChargeAttack retValue = new ChargeAttack(attackingFigure, defendingFigure, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }
    public void copyComponentTo(ChargeAttack target) {
        super.copyComponentTo(target);
    }
}
