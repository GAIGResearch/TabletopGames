package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.getAttackType;

public class DeathRageAttack extends MeleeAttack {
    public DeathRageAttack(int attackingFigure, int defendingFigure, boolean reach) {
        super(attackingFigure, defendingFigure, reach);
    }

    public boolean execute(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        attacker.getAttribute(Figure.Attribute.Fatigue).increment(2);

        SurgeAttackAction deathRage = new SurgeAttackAction(Surge.DEATH_RAGE, attackingFigure);
        attacker.addAbility(deathRage);

        super.execute(state);

        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 2 > f.getAttribute(Figure.Attribute.Fatigue).getMaximum()
                || f.getNActionsExecuted().isMaximum()) return false;
        DescentTypes.AttackType attackType = getAttackType(f);
        return (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH) && super.canExecute(dgs);
    }

    @Override
    public void executePhase(DescentGameState dgs) {
        // Just make sure we remove the Death Rage surge so it can't be accidentally used by other attacks
        if (phase == AttackPhase.PRE_DEFENCE_ROLL) {
            Figure attacker = (Figure) dgs.getComponentById(attackingFigure);
            attacker.removeAbility(new SurgeAttackAction(Surge.DEATH_RAGE, attackingFigure));
        }
        super.executePhase(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Death Rage: " + super.getString(gameState));
    }

    public DeathRageAttack copy() {
        DeathRageAttack retValue = new DeathRageAttack(attackingFigure, defendingFigure, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public String toString() {
        return String.format("Death Rage Attack by %d on %d", attackingFigure, defendingFigure);
    }
}
