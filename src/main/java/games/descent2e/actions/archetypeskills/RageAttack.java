package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.getAttackType;

public class RageAttack extends MeleeAttack {
    public RageAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    public boolean execute(DescentGameState state) {
        addDamage(1);
        super.execute(state);
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        attacker.getAttribute(Figure.Attribute.Fatigue).increment();
        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum() || f.getNActionsExecuted().isMaximum()) return false;
        DescentTypes.AttackType attackType = getAttackType(f);
        return (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH) && super.canExecute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Rage: " + super.getString(gameState));
    }

    public RageAttack copy() {
        RageAttack retValue = new RageAttack(attackingFigure, defendingFigure);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public String toString() {
        return String.format("Rage Attack by %d on %d", attackingFigure, defendingFigure);
    }
}
