package pandemic.engine.conditions;

import core.GameState;
import pandemic.engine.Node;

public class ActionsPerTurnPlayed extends ConditionNode {
    int n_actions;

    public ActionsPerTurnPlayed(int n_actions, Node yes, Node no) {
        super(yes, no);
        this.n_actions = n_actions;
    }

    @Override
    public boolean test(GameState gs) {
        return gs.roundStep == n_actions;
    }
}
