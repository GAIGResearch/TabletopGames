package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.hasLineOfSight;
import static games.descent2e.DescentHelper.inRange;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;

/**
 *   This works in exactly the same way as a Melee Attack
 *   Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class MultiAttack extends RangedAttack {

    public List<Integer> defendingFigures;
    int index;

    public MultiAttack(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures.get(0));
        this.defendingFigures = new ArrayList<>(defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState state) {
        defendingFigure = defendingFigures.get(0);
        index = 0;
        super.execute(state);

        // remove final "; Result: " from result
        result = result.substring(0, result.length() - 10);
        for (int i = 1; i < defendingFigures.size(); i++) {
            result += " & " + (state.getComponentById(defendingFigures.get(i)).getComponentName().replace("Hero: ", ""));
        }
        result += "; Result: ";

        return true;
    }

    private void setNewTarget(DescentGameState state, int index)
    {
        defendingFigure = defendingFigures.get(index);
        super.setDefendingFigure(defendingFigures.get(index));
        Figure defender = (Figure) state.getComponentById(defendingFigure);

        defender.setCurrentAttack(this);

        DicePool defencePool = defender.getDefenceDice();
        state.setDefenceDicePool(defencePool);

        if (defender instanceof Monster) {
            if (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.NIGHTSTALKER))
            {
                NightStalker.addNightStalker(state, ((Figure) state.getComponentById(attackingFigure)).getPosition(), defender.getPosition());
            }
        }

        //System.out.println("Next target (" + (index+1) + "/" + defendingFigures.size() + "): " + defender.getComponentName());
    }

    @Override
    void executePhase(DescentGameState state) {
        switch (phase) {
            case POST_DAMAGE:
                applyDamage(state);
                phase = NEXT_TARGET;
                break;
            case NEXT_TARGET:
                if(index < defendingFigures.size() - 1)
                {
                    index++;
                    setNewTarget(state, index);
                    result += " | Result: ";
                    // For MultiAttacks, we use the same Attack dice results
                    // But each defender is allowed to roll their own Defence dice
                    phase = PRE_DEFENCE_ROLL;
                }
                else
                {
                    ((Figure) state.getComponentById(attackingFigure)).addActionTaken(toStringWithResult());
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
                string += " and ";
            }
        }

        string += "; " + result;

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
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;
            if(!inRange(f.getPosition(), target.getPosition(), MAX_RANGE)) return false;
            if(!hasLineOfSight(dgs, f.getPosition(), target.getPosition())) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String string = String.format("Multi Attack by %d on ", attackingFigure);
        for (int i = 0; i < defendingFigures.size(); i++) {
            string += defendingFigures.get(i);
            if (i < defendingFigures.size() - 1) {
                string += " and ";
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
        return index == that.index && Objects.equals(defendingFigures, that.defendingFigures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defendingFigures, index);
    }

    @Override
    public MultiAttack copy() {
        MultiAttack retValue = new MultiAttack(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(MultiAttack target) {
        target.index = index;
        target.defendingFigure = defendingFigure;  // we also need to set this, as the constructor overrides it
        super.copyComponentTo(target);
    }
}
