package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.components.Dice;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.getAttackType;

public class BareHandAttack extends MeleeAttack {

    public BareHandAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure, false);
    }

    public boolean execute(DescentGameState state) {
        super.execute(state);
        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f == null) return false;
        if (getAttackType(f) != DescentTypes.AttackType.NONE) return false;
        return super.canExecute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Bare Hand " + super.getString(gameState));
    }

    public BareHandAttack copy() {
        BareHandAttack retValue = new BareHandAttack(attackingFigure, defendingFigure);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public String toString() {
        return String.format("Bare Hand Melee Attack by %d on %d", attackingFigure, defendingFigure);
    }
}
