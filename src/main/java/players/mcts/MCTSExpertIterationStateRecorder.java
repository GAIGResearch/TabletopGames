package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.StateFeatureListener;
import evaluation.metrics.Event;

import java.util.Map;

/*
 * This records the state values as estimated by the MCTS expert iteration (the oracle is in the parent MCTSExpertIterationListener).
 */
public class MCTSExpertIterationStateRecorder extends StateFeatureListener {

    private final MCTSExpertIterationListener parent;

    public MCTSExpertIterationStateRecorder(MCTSExpertIterationListener parent) {
        super(parent.getPhiFn(), Event.GameEvent.ACTION_CHOSEN, true);
        this.parent = parent;
    }

    @Override
    public void processState(AbstractGameState state, AbstractAction action) {
        if (action == null) return; // we do not record data for the GAME_OVER event

        super.processState(state, action);
        double stateValue = parent.getOracle().root.nodeValue(state.getCurrentPlayer());
        overrideData.add(Map.of("FinalScore", stateValue, "Win", stateValue));
    }

}
