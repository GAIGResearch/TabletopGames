package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Deck;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Arrays;
import java.util.Objects;

import static games.descent2e.DescentHelper.checkAllSpaces;

public class DirtyTricks extends RangedAttack {

    protected boolean isMelee;

    public DirtyTricks(int attackingFigure, int defendingFigure) {
        this(attackingFigure, defendingFigure, false, false);
    }

    public DirtyTricks(int attackingFigure, int defendingFigure, boolean isMelee, boolean reach) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
        this.hasReach = reach;
    }

    @Override
    public boolean execute(DescentGameState state) {
        setStunning(true);
        super.execute(state);
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        attacker.getAttribute(Figure.Attribute.Fatigue).increment();
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;

        // If it's a melee attack, by definition it is a melee weapon
        // So, we only check if it's a Blade weapon if it's a ranged attack
        if (!isMelee)
        {
            if (!(f instanceof Hero hero)) return false;
            boolean blade = false;
            Deck<DescentCard> hand = hero.getHandEquipment();
            if (hand == null) return false;
            for (DescentCard item : hand.getComponents()) {
                String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                if (equipmentType == null) continue;
                if (Arrays.asList(equipmentType).contains("Blade")) {
                    blade = true;
                    break;
                }
            }
            if (!blade) return false;
        }

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        return checkAllSpaces(dgs, f, target, getRange(), true);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DirtyTricks other) {
            if (other.isMelee == this.isMelee)
                return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isMelee);
    }


    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        if (isMelee)
        {
            return String.format("Dirty Tricks: Melee Attack by " + attackerName + " on " + defenderName + "; " + result);
        }

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Dirty Tricks: Ranged Attack by " + attackerName + " on " + defenderName + " (Range: " + distance + "); " + result);
    }

    @Override
    public String toString() {
        if (isMelee) return String.format("Dirty Tricks Melee Attack by %d on %d", attackingFigure, defendingFigure);
        return String.format("Dirty Tricks Ranged Attack by %d on %d", attackingFigure, defendingFigure);
    }

    public DirtyTricks copy() {
        DirtyTricks retValue = new DirtyTricks(attackingFigure, defendingFigure, isMelee, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(DirtyTricks target) {
        super.copyComponentTo(target);
        target.isMelee = isMelee;
    }
}
