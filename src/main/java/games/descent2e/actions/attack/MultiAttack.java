package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.actions.monsterfeats.NotMe;
import games.descent2e.actions.monsterfeats.NotMeSwap;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;

/**
 *   This works in exactly the same way as a Melee Attack
 *   Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class MultiAttack extends RangedAttack {

    public List<Integer> defendingFigures;
    protected int index;
    protected int swapIndex;

    public MultiAttack(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures.get(0));
        this.defendingFigures = new ArrayList<>(defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState state) {

        // We need to check all defending figures to see if they are Monsters with the Shadow Passive
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        for (int target : defendingFigures) {
            if (!hasShadow) {
                Figure defender = (Figure) state.getComponentById(target);
                if (checkShadow(state, attacker, defender)) {
                    hasShadow = true;
                    SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
                    if (!attacker.getAbilities().contains(shadowSurge)) {
                        attacker.addAbility(new SurgeAttackAction(Surge.SHADOW, attackingFigure));
                    }
                }
            }
            else break;
        }

        // If no targets have the Shadow Passive, remove the ability to use the Shadow Surge
        if (!hasShadow)
        {
            // Only enable the Shadow Surge if the target has the Shadow passive
            SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
            if (attacker.getAbilities().contains(shadowSurge)) {
                attacker.removeAbility(shadowSurge);
            }
        }

        defendingFigure = defendingFigures.get(0);
        index = 0;
        super.execute(state);

        return true;
    }

    @Override
    protected void checkSubstitute(DescentGameState state)
    {
        // Everything we need to do is handled elsewhere in the code, except for the first defender
        if (substitute)
            if (swapIndex == 0)
            {
                defendingFigures.set(swapIndex, substituteFigure);
                super.checkSubstitute(state);
            }
    }

    private void setNewTarget(DescentGameState state, int index)
    {
        boolean swap = false;

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure oldDefender = (Figure) state.getComponentById(defendingFigures.get(index));

        if (substitute && index == swapIndex)
        {
            defendingFigures.set(swapIndex, substituteFigure);
            swap = true;
        }
        defendingFigure = defendingFigures.get(index);
        super.setDefendingFigure(defendingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        defendingPlayer = defender.getOwnerId();

        defender.setCurrentAttack(this);

        DicePool defencePool = defender.getDefenceDice();
        state.setDefenceDicePool(defencePool);

        // Check again for Night Stalker passive
        if (!checkAdjacent(state, attacker, oldDefender)) {
            if (!swap) {
                NightStalker.addNightStalker(state, attacker, defender);
            }
            else
            {
                if ((defender instanceof Monster) && (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.NIGHTSTALKER)))
                {
                    NightStalker.addPool(state);
                }
            }
        }

        if (defender instanceof Hero) getWeaponBonuses(state, defendingFigure, true, false);
        // if (defender instanceof Monster && ((Monster) defender).isLieutenant()) getWeaponBonuses(state, defendingFigure, false, false);


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

                    // When dealing with substitutes for attacks, our targets might include an already defeated Figure
                    // So, we skip it if so
                    if (state.getComponentById(defendingFigures.get(index)) == null)
                        break;

                    setNewTarget(state, index);
                    result += " | Result: ";
                    // For MultiAttacks, we use the same Attack dice results
                    // But each defender is allowed to roll their own Defence dice
                    phase = PRE_DEFENCE_ROLL;
                }
                else
                {
                    ((Figure) state.getComponentById(attackingFigure)).addActionTaken(toStringWithResult());
                    phase = INTERRUPT_ATTACK;
                }
                break;
            default:
                super.executePhase(state);
                break;
        }
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

        if (action instanceof NotMe) {
            DescentGameState dgs = (DescentGameState) state;
            substituteFigure = ((NotMe) action).getVictim();

            // Only swap if the defender chose to swap
            if (((NotMe) action).getResult() == 1 && substituteFigure != -1 && defendingFigure != substituteFigure) {

                substitute = true;

                swapIndex = defendingFigures.indexOf(defendingFigure);

                Figure attacker = (Figure) state.getComponentById(attackingFigure);
                Figure splig = (Figure) state.getComponentById(defendingFigure);
                Figure defender = (Figure) state.getComponentById(substituteFigure);
                substitutePlayer = defender.getOwnerId();
                substituteName = defender.getName().replace("Hero: ", "");

                if (checkAdjacent(dgs, attacker, splig)) {
                    if (!hasShadow)
                        if ((attacker instanceof Hero) && (defender instanceof Monster) && (((Monster) defender).hasPassive(MonsterAbilities.MonsterPassive.SHADOW))) {
                            hasShadow = true;
                            SurgeAttackAction shadowSurge = new SurgeAttackAction(Surge.SHADOW, attackingFigure);
                            if (!attacker.getAbilities().contains(shadowSurge))
                                attacker.addAbility(new SurgeAttackAction(Surge.SHADOW, attackingFigure));
                        }
                }
            }
        }

        // After the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
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

            if (!isMelee) {
                Double range = getDistanceFromFigures(attacker, defender);
                if (range > 1.0) {
                    String distance = Double.toString(range);
                    string += " (Range: " + distance + ")";
                }
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

        if (!isFreeAttack) {
            if (f.getNActionsExecuted().isMaximum()) return false;
        }

        for (int defendingFigure : defendingFigures)
        {
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;

            if (target instanceof Monster)
            {
                if (((Monster) target).hasPassive(MonsterAbilities.MonsterPassive.AIR) &&
                        !checkAdjacent(dgs, f, target)) {
                    // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
                    return false;
                }
            }

            if (!checkAllSpaces(dgs, f, target, getRange(), true)) return false;
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
        return index == that.index && swapIndex == that.swapIndex && Objects.equals(defendingFigures, that.defendingFigures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defendingFigures, index, swapIndex);
    }

    @Override
    public MultiAttack copy() {
        MultiAttack retValue = new MultiAttack(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(MultiAttack target) {
        target.index = index;
        target.swapIndex = swapIndex;
        target.defendingFigure = defendingFigure;  // we also need to set this, as the constructor overrides it
        super.copyComponentTo(target);
    }

    @Override
    public String getInitialResult(DescentGameState dgs)
    {
        String retVal = super.getInitialResult(dgs);
        // Remove final "; Result: " from result
        retVal = retVal.substring(0, retVal.length() - 10);
        for (int i = 1; i < defendingFigures.size(); i++) {
            int target = defendingFigures.get(i);
            if (substitute && i == swapIndex) target = substituteFigure;
            retVal += " & " + dgs.getComponentById(target).getComponentName().replace("Hero: ", "");
        }
        retVal += "; Result: ";
        return retVal;
    }
}
