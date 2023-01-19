package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;

public class ActionsPerTurnPlayed extends ConditionNode {
    int n_actions;

    public ActionsPerTurnPlayed(int n_actions) {
        super();
        this.n_actions = n_actions;
    }

    /**
     * Copy constructor
     * @param actionsPerTurnPlayed - Node to be copied
     */
    public ActionsPerTurnPlayed(ActionsPerTurnPlayed actionsPerTurnPlayed) {
        super(actionsPerTurnPlayed);
        this.n_actions = actionsPerTurnPlayed.n_actions;
    }

    @Override
    public boolean test(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;
        return ((PandemicTurnOrder)pgs.getTurnOrder()).getTurnStep() >= n_actions;
    }

    @Override
    protected Node _copy() {
        return new ActionsPerTurnPlayed(this);
    }
}
