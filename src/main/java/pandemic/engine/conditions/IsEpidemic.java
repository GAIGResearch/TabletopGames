package pandemic.engine.conditions;

import core.GameState;
import pandemic.PandemicGameState;

public class IsEpidemic extends ConditionNode {
    @Override
    public boolean test(GameState gs) {
        return ((PandemicGameState)gs).isEpidemic();
    }
}
