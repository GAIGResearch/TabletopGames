package games.descent2e.actions.herofeats;

import breeze.util.ArrayUtil;
import core.AbstractGameState;
import core.components.Deck;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Arrays;
import java.util.List;

import static games.descent2e.DescentHelper.getMeleeTargets;

public class MagicAttackAll extends DescentAction {
    public MagicAttackAll() {
        super(Triggers.HEROIC_FEAT);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        DescentAction heroicFeat = constructHeroicFeat(dgs);
        if (heroicFeat != null) {
            return heroicFeat.execute(dgs);
        }
        return false;
    }

    private DescentAction constructHeroicFeat(DescentGameState dgs) {
        List<Integer> monsters = getMeleeTargets(dgs, dgs.getActingFigure());
        if (!monsters.isEmpty()) {
            DescentAction heroicFeat = new AttackAllAdjacent(dgs.getActingFigure().getComponentID(), monsters);
            if (heroicFeat.canExecute(dgs)) {
                return heroicFeat;
            }
        }
        return null;
    }

    @Override
    public MagicAttackAll copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only use this if we attack with a Magic weapon
        if (f instanceof Hero) {
            Deck<DescentCard> hand = ((Hero) f).getHandEquipment();
            if (hand == null) return false;
            boolean hasMagicItem = false;
            for (DescentCard item: hand.getComponents())
            {
                String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                if (equipmentType == null) continue;
                if (Arrays.asList(equipmentType).contains("Magic")) {
                    hasMagicItem = true;
                    break;
                }
            }
            if (!hasMagicItem) return false;
        }
        DescentAction heroicFeat = constructHeroicFeat(dgs);
        return heroicFeat != null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MagicAttackAll && super.equals(o);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Magic attack all adjacent monsters";
    }

    public String toString() {
        return "Heroic Feat: Magic Attack All";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 112002;
    }
}
