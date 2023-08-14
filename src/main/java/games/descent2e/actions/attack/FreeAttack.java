package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.HashSet;

import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.PRE_ATTACK_ROLL;

public class FreeAttack extends RangedAttack{

    protected boolean isMelee;
    public FreeAttack(int attackingFigure, int defendingFigure, boolean isMelee) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
    }

    @Override
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

        // This is only applicable for Ranged Attacks
        if (!isMelee) {
            if (defender instanceof Monster) {
                if (((Monster) defender).hasPassive("NightStalker")) {
                    NightStalker.addNightStalker(state, attacker, defender);
                }
            }
        }

        super.movePhaseForward(state);

        // Remove the ability to use the Extra Action this turn
        attacker.setUsedExtraAction(true);

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;
    }

    public FreeAttack copy() {
        FreeAttack retValue = new FreeAttack(attackingFigure, defendingFigure, isMelee);
        retValue.attackingPlayer = attackingPlayer;
        retValue.defendingPlayer = defendingPlayer;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        retValue.surgesToSpend = surgesToSpend;
        retValue.extraRange = extraRange;
        retValue.extraDamage = extraDamage;
        retValue.mending = mending;
        retValue.surgesUsed = new HashSet<>(surgesUsed);
        retValue.pierce = pierce;
        retValue.isDiseasing = isDiseasing;
        retValue.isImmobilizing = isImmobilizing;
        retValue.isPoisoning = isPoisoning;
        retValue.isStunning = isStunning;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FreeAttack) {
            FreeAttack other = (FreeAttack) obj;
            if (other.isMelee == this.isMelee)
                return super.equals(obj);
        }
        return false;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        if (isMelee)
        {
            return String.format("Free Attack (Melee) by " + attackerName + " on " + defenderName);
        }

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Free Attack (Ranged) by " + attackerName + " on " + defenderName + " (Range: " + distance + ")");
    }

    @Override
    public String toString() {
        if (isMelee) return String.format("Free Attack (Melee) by %d on %d", attackingFigure, defendingFigure);
        return String.format("Free Attack (Ranged) by %d on %d", attackingFigure, defendingFigure);
    }
}
