package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.components.Deck;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class AttackAllAdjacent extends MultiAttack {

    boolean heroicFeat;

    public AttackAllAdjacent(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
        this.minRange = Integer.MIN_VALUE;
        this.isMelee = true;
        this.hasReach = false;
        this.heroicFeat = false;
    }

    public AttackAllAdjacent(int attackingFigure, List<Integer> defendingFigures, boolean heroicFeat) {
        super(attackingFigure, defendingFigures);
        this.minRange = Integer.MIN_VALUE;
        this.isMelee = true;
        this.hasReach = false;
        this.heroicFeat = heroicFeat;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure hero = (Figure) dgs.getComponentById(attackingFigure);
        if (heroicFeat)
            if (hero instanceof Hero)
                ((Hero) hero).setFeatAvailable(false);
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (defendingFigures.isEmpty()) return false;
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f instanceof Hero)
        {
            if (heroicFeat && !((Hero) f).isFeatAvailable()) return false;
            Deck<DescentCard> hand = ((Hero) f).getHandEquipment();
            if (hand == null || hand.getSize() == 0) return false;
            boolean hasMagicItem = false;
            for (DescentCard item : hand.getComponents()) {
                String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                if (equipmentType == null) continue;
                if (Arrays.asList(equipmentType).contains("Magic")) {
                    hasMagicItem = true;
                    break;
                }
            }
            if (!hasMagicItem) return false;
        }
        for (int defendingFigure : defendingFigures)
        {
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;

            if (!checkAdjacent(dgs, f, target)) return false;
        }
        return true;
    }

    public AttackAllAdjacent copy() {
        AttackAllAdjacent retValue = new AttackAllAdjacent(attackingFigure, defendingFigures, heroicFeat);
        copyComponentTo(retValue);
        return retValue;
    }

    public void copyComponentTo(AttackAllAdjacent target) {
        super.copyComponentTo(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Attack all adjacent monsters; " + result;
    }

    @Override
    public String toString() {
        return "Heroic Feat: Attack all adjacent monsters";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AttackAllAdjacent that) {
            return super.equals(obj) && this.heroicFeat == that.heroicFeat;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroicFeat);
    }

}
