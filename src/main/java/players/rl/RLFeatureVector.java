package players.rl;

import java.util.HashMap;
import java.util.Map;

import core.interfaces.IActionFeatureVector;

/**
 * RLPlayer uses this feature vector. It links the player back to the feature
 * vector, in case methods like player.getForwardModel() need to be used.
 */
public abstract class RLFeatureVector implements IActionFeatureVector {

    protected Map<Integer, RLPlayer> players;

    protected RLFeatureVector() {
        players = new HashMap<>();
    }

    void linkPlayer(RLPlayer player) {
        players.put(player.getPlayerID(), player);
    }

}
