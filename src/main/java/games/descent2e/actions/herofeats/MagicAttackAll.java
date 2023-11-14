package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

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
        DescentAction heroicFeat = constructHeroicFeat(dgs);
        return heroicFeat != null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MagicAttackAll && super.equals(o);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Magic attack all adjacent monsters.";
    }
}
