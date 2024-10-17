package players.mcts;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;

import java.util.List;

public class LMRForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return List.of(new LMRAction("Left"), new LMRAction("Middle"), new LMRAction("Right"));
    }
}

class LMRAction extends AbstractAction {

    String name;

    public LMRAction(String name) {
        this.name = name;
    }
    @Override
    public boolean execute(AbstractGameState gameState) {
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LMRAction && ((LMRAction) obj).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return name;
    }
}
