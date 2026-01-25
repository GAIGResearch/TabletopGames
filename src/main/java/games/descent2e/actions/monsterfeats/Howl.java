package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.components.Figure;

import java.util.List;

public class Howl extends TriggerAttributeTest{
    public Howl(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int defendingFigure = heroes.get(currentIndex);
        return List.of(new HowlTest(defendingFigure, Figure.Attribute.Willpower, attackingFigure));
    }

    @Override
    TriggerAttributeTest _copy() {
        return new Howl(attackingFigure, heroes);
    }
}
