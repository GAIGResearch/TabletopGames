package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;

public class IsEpidemic extends ConditionNode {
    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicGameState)gs).isEpidemic();
    }
}
