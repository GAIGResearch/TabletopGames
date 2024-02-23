package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.getAttackType;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;

public class RageAttack extends MeleeAttack {
    public RageAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = attackingPlayer;
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        DicePool attackPool = attacker.getAttackDice();
        DicePool defencePool = defender.getDefenceDice();

        state.setAttackDicePool(attackPool);
        state.setDefenceDicePool(defencePool);

        super.addDamage(1);

        super.movePhaseForward(state);

        attacker.getNActionsExecuted().increment();
        attacker.getAttribute(Figure.Attribute.Fatigue).increment();
        attacker.setHasAttacked(true);

        // When executing a melee attack we need to:
        // 1) roll the dice (with possible interrupt beforehand)
        // 2) Possibly invoke re-roll options (via interrupts)
        // 3) and then - if there are any surges - decide how to use them
        // 4) and then get the target to roll their defence dice
        // 5) with possible rerolls
        // 6) then do the damage
        // 7) target can use items/abilities to modify damage

        return true;
    }

    public boolean canExecute(DescentGameState dgs)
    {
       Figure f = (Figure) dgs.getComponentById(attackingFigure);
       if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum() || f.getNActionsExecuted().isMaximum()) return false;
       DescentTypes.AttackType attackType = getAttackType(f);
       return (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH) && super.canExecute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();

        // Sometimes the game will remove the dead enemy off the board before
        // it can state in the Action History the attack that killed them
        if (gameState.getComponentById(defendingFigure) != null) {
            defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        }
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");
        return String.format("Rage: Melee Attack by " + attackerName + " on " + defenderName + " (+1 Damage, +1 Fatigue); " + result);
        //return toString();
        // TODO: Extend this to pull in details of card and figures involved
    }

    public RageAttack copy()
    {
        RageAttack retValue = new RageAttack(attackingFigure, defendingFigure);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public String toString() {
        return String.format("Rage Attack by %d on %d", attackingFigure, defendingFigure);
    }
}
