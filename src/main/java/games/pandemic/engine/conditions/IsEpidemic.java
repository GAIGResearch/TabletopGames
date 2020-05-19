package games.pandemic.engine.conditions;

import core.AbstractGameState;
import games.pandemic.PandemicGameState;

public class IsEpidemic extends ConditionNode {
    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicGameState)gs).isEpidemic();
    }
}
