package pandemic.engine.conditions;

import core.GameState;
import pandemic.PandemicGameState;
import pandemic.engine.Node;

public class IsEpidemic extends ConditionNode {
    public IsEpidemic(Node yes, Node no) {
        super(yes, no);
    }

    @Override
    public boolean test(GameState gs) {
        return ((PandemicGameState)gs).isEpidemic();
    }
}
