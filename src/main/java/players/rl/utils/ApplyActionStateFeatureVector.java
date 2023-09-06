package players.rl.utils;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import players.rl.RLPlayer;

public class ApplyActionStateFeatureVector implements IActionFeatureVector {

    private IStateFeatureVector stateFeatureVector;
    protected RLPlayer player;

    public ApplyActionStateFeatureVector(IStateFeatureVector stateFeatureVector) {
        this.stateFeatureVector = stateFeatureVector;
    }

    public void linkPlayer(RLPlayer player) {
        this.player = player;
    }

    @Override
    public final double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        AbstractGameState nextState = state.copy(playerID);
        player.getForwardModel().next(nextState, action);
        return stateFeatureVector.featureVector(nextState, playerID);
    }

    @Override
    public String[] names() {
        return stateFeatureVector.names();
    }

    public String getFeatureVectorCanonicalName() {
        return stateFeatureVector.getClass().getCanonicalName();
    }

}
