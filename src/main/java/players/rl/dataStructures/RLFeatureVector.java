package players.rl.dataStructures;

import java.util.HashMap;
import java.util.Map;

import core.interfaces.IActionFeatureVector;
import players.rl.RLPlayer;

/**
 * RLPlayer uses this feature vector. It links the player back to the feature
 * vector, in case methods like player.getForwardModel() need to be used.
 */
public abstract class RLFeatureVector implements IActionFeatureVector {

    Map<Integer, RLPlayer> players;

    RLFeatureVector() {
        players = new HashMap<>();
    }

    public void linkPlayer(RLPlayer player) {
        players.put(player.getPlayerID(), player);
    }

}
