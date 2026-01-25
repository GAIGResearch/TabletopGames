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

import static games.descent2e.DescentHelper.inRange;

public class AttackAllAdjacent extends MultiAttack {

    public AttackAllAdjacent(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        // TODO: current acting figure is doing this, find it
        // TODO: find all adjacent enemies next to the figure and attack them as in super class
        dgs.setActionInProgress(this);
        if (dgs.getActingFigure() instanceof Hero) {((Hero) dgs.getActingFigure()).setFeatAvailable(false); }
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (defendingFigures.size() < 1) return false;
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f instanceof Hero)
        {
            if (!((Hero) f).isFeatAvailable()) return false;
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
            if(!inRange(f.getPosition(), ((Figure) dgs.getComponentById(defendingFigure)).getPosition(), 1)) return false;
        }
        return true;
    }

    public AttackAllAdjacent copy() {
        AttackAllAdjacent retValue = new AttackAllAdjacent(attackingFigure, defendingFigures);
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

}
