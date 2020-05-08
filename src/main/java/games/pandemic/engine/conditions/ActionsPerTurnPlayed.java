package games.pandemic.engine.conditions;

import core.AbstractGameState;
import games.pandemic.PandemicTurnOrder;

public class ActionsPerTurnPlayed extends ConditionNode {
    int n_actions;

    public ActionsPerTurnPlayed(int n_actions) {
        super();
        this.n_actions = n_actions;
    }

    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicTurnOrder)gs.getTurnOrder()).getTurnStep() == n_actions;
    }
}
