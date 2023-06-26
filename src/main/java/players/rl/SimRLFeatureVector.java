package players.rl;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;

/**
 * This class simply simulates the given action by applying it in the forward
 * model, and then acts analog to an IStateFeatureVector for the nextState
 */
public abstract class SimRLFeatureVector extends RLFeatureVector implements IStateFeatureVector {

    @Override
    public double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        AbstractGameState nextState = state.copy(playerID);
        players.get(playerID).getForwardModel().next(nextState, action);
        return featureVector(nextState, playerID);
    }

}
