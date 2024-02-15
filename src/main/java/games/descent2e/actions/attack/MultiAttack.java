package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.inRange;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;

/**
 *   This works in exactly the same way as a Melee Attack
 *   Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class MultiAttack extends RangedAttack {

    int defendingFigure;
    public List<Integer> defendingFigures;
    int index;

    public MultiAttack(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures.get(0));
        this.defendingFigures = defendingFigures;
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);

        defendingFigure = defendingFigures.get(0);

        index = 0;

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

        if (defender instanceof Monster) {
            if (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.NIGHTSTALKER))
            {
                NightStalker.addNightStalker(state, attackingFigure, defendingFigure);
            }
        }

        super.movePhaseForward(state);

        attacker.getNActionsExecuted().increment();
        attacker.setHasAttacked(true);

        attacker.addActionTaken(toString());

        return true;
    }

    private void setNewTarget(DescentGameState state, int index)
    {
        defendingFigure = defendingFigures.get(index);
        Figure defender = (Figure) state.getComponentById(defendingFigure);

        defender.setCurrentAttack(this);

        DicePool defencePool = defender.getDefenceDice();
        state.setDefenceDicePool(defencePool);

        if (defender instanceof Monster) {
            if (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.NIGHTSTALKER))
            {
                NightStalker.addNightStalker(state, attackingFigure, defendingFigure);
            }
        }

        //System.out.println("Next target (" + (index+1) + "/" + defendingFigures.size() + "): " + defender.getComponentName());
    }

    @Override
    void executePhase(DescentGameState state) {
        switch (phase) {
            case POST_DAMAGE:
                super.applyDamage(state);
                phase = NEXT_TARGET;
                break;
            case NEXT_TARGET:
                if(index < defendingFigures.size() - 1)
                {
                    index++;
                    setNewTarget(state, index);
                    // For MultiAttacks, we use the same Attack dice results
                    // But each defender is allowed to roll their own Defence dice
                    phase = PRE_DEFENCE_ROLL;
                }
                else
                {
                    phase = ALL_DONE;
                }
                break;
            default:
                super.executePhase(state);
                break;
        }
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);

        attackerName = attacker.getComponentName().replace("Hero: ", "");

        String string = String.format("Multi Attack by " + attackerName + " on ");

        for (int i = 0; i < defendingFigures.size(); i++) {
            Figure defender = (Figure) gameState.getComponentById(defendingFigures.get(i));
            defenderName = defender.getComponentName().replace("Hero: ", "");
            string += defenderName;

            Double range = getDistanceFromFigures(attacker, defender);
            if (range > 1.0)
            {
                String distance = Double.toString(range);
                string += " (Range: " + distance + ")";
            }

            if (i < defendingFigures.size() - 1) {
                string += ", ";
            }
        }

        return string;
        //return toString();
        // TODO: Extend this to pull in details of card and figures involved, including distance
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;
        for (int defendingFigure : defendingFigures)
        {
            if(!inRange(f.getPosition(), ((Figure) dgs.getComponentById(defendingFigure)).getPosition(), MAX_RANGE)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String string = String.format("Multi Attack by %d on ", attackingFigure);
        for (int i = 0; i < defendingFigures.size(); i++) {
            string += defendingFigures.get(i);
            if (i < defendingFigures.size() - 1) {
                string += ", ";
            }
        }
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MultiAttack that = (MultiAttack) o;
        return defendingFigure == that.defendingFigure && index == that.index && Objects.equals(defendingFigures, that.defendingFigures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defendingFigure, defendingFigures, index);
    }

    @Override
    public MultiAttack copy() {
        MultiAttack retValue = new MultiAttack(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(MultiAttack target) {
        target.defendingFigure = defendingFigure;
        target.index = index;
        super.copyComponentTo(target);
    }
}
