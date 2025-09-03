package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.archetypeskills.PrayerOfPeace;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Distance;

import static games.descent2e.DescentHelper.*;

/**
 * This works in exactly the same way as a Melee Attack
 * Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class RangedAttack extends MeleeAttack {

    // How many tiles we check at maximum for Ranged Attacks
    public static final int MAX_RANGE = 8;

    public RangedAttack(int attackingFigure, int defendingFigure) {
        // Ranged Attacks can never have Reach, only Melee attacks can
        super(attackingFigure, defendingFigure, false);
        this.isMelee = false;
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        return true;
    }

    @Override
    public boolean attackMissed(DescentGameState state) {
        if (super.attackMissed(state))
            return true; // due to no damage done

        // We have children inheriting from RangedAttack which are technically MeleeAttacks
        // For normal RangedAttacks we ignore this following check and continue
        // But for any children that are MeleeAttacks, we can stop here
        if (isMelee) return false;

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        if (defender == null) {
            return true; // Somehow, we have an attack on a dead figure
        }
        double distance = getDistanceFromFigures(attacker, defender) + minRange;
        range = state.getAttackDicePool().getRange() + extraRange;
        return (range < distance);
    }

    public double getDistanceFromFigures(Figure attacker, Figure defender) {
        // TODO: Chebyshev distance is not actually right, as it does not allow diagonal moves
        return Distance.chebyshev_distance(attacker.getPosition(), defender.getPosition());
    }

    @Override
    public String shortString(AbstractGameState gameState) {
        // TODO: Extend this to pull in details of card and figures involved, including distance
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Ranged Attack by " + attackerName + " on " + defenderName + " (Range: " + distance + ")" + "; " + result);
    }

    @Override
    public String longString(AbstractGameState gameState) {

        attackerName = gameState.getComponentById(attackingFigure).getComponentName();

        // Sometimes the game will remove the dead enemy off the board before
        // it can state in the Action History the attack that killed them
        if (gameState.getComponentById(defendingFigure) != null) {
            defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        }
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Ranged Attack by " + attackerName + "(" + attackingPlayer
                + ") on " + defenderName + "(" + defendingPlayer + "). "
                + "Phase: " + phase + ". Interrupt player: " + interruptPlayer
                + ". Range: " + distance
                + ". Surges to spend: " + surgesToSpend
                + ". Extra range: " + extraRange
                + ". Pierce: " + pierce
                + ". Extra damage: " + extraDamage
                + ". Extra defence: " + extraDefence
                + ". Mending: " + mending
                + ". Disease: " + isDiseasing
                + ". Immobilize: " + isImmobilizing
                + ". Poison: " + isPoisoning
                + ". Stun: " + isStunning
                + ". Damage: " + damage
                + ". Skip: " + skip
                + ". Surges used: " + surgesUsed.toString()
        );
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;

        // Check for abilities that prevent attacking
        if (!PrayerOfPeace.canAttackPrayer(dgs, f)) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        return checkAllSpaces(dgs, f, target, getRange(), true);
    }

    @Override
    protected int getRange() {
        int range = MAX_RANGE;
        if (isMelee) {
            range = 1;
        }
        if (hasReach) {
            range = 2;
        }
        return range;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RangedAttack) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public RangedAttack copy() {
        RangedAttack retValue = new RangedAttack(attackingFigure, defendingFigure);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(RangedAttack target) {
        super.copyComponentTo(target);
    }

    @Override
    public String toString() {
        return String.format("Ranged Attack by %d on %d", attackingFigure, defendingFigure);
    }
}
