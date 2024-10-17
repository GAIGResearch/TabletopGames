package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;

public class IsEpidemic extends ConditionNode {

    public IsEpidemic() {
        super();
    }

    /**
     * Copy constructor
     * @param isEpidemic - Node to be copied
     */
    public IsEpidemic(IsEpidemic isEpidemic) {
        super(isEpidemic);
    }

    @Override
    public boolean test(AbstractGameState gs) {
        return ((PandemicGameState)gs).isEpidemic();
    }

    @Override
    protected Node _copy() {
        return new IsEpidemic(this);
    }
}
