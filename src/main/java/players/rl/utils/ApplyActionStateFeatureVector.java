package players.rl.utils;

import java.util.HashMap;
import java.util.Map;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import players.rl.RLPlayer;

public class ApplyActionStateFeatureVector implements IActionFeatureVector {

    private IStateFeatureVector stateFeatureVector;
    protected Map<Integer, RLPlayer> players;

    public ApplyActionStateFeatureVector(IStateFeatureVector stateFeatureVector) {
        this.stateFeatureVector = stateFeatureVector;
        players = new HashMap<>();
    }

    public void linkPlayer(RLPlayer player) {
        players.put(player.getPlayerID(), player);
    }

    @Override
    public final double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        AbstractGameState nextState = state.copy(playerID);
        players.get(playerID).getForwardModel().next(nextState, action);
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
