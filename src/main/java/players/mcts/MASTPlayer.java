package players.mcts;

import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import players.simple.BoltzmannActionPlayer;
import utilities.Pair;

import java.util.List;
import java.util.Map;

public class MASTPlayer extends BoltzmannActionPlayer implements IMASTUser {

    // A constructor to just use MAST
    public MASTPlayer(IActionKey actionKey, double temperature, double epsilon, long seed, double defaultValue) {
        super(new MASTActionHeuristic(actionKey, defaultValue), temperature, epsilon, seed);
    }

    // Or we can blend in an external heuristic
    public MASTPlayer(IActionHeuristic externalHeuristic, double weightOfExternal, IActionKey actionKey, double temperature, double epsilon, double defaultValue) {
        super(new MASTPlusActionHeuristic(externalHeuristic, actionKey, defaultValue, weightOfExternal), temperature, epsilon, System.currentTimeMillis());
    }

    @Override
    public void setMASTStats(List<Map<Object, Pair<Integer, Double>>> MASTStats) {
        ((IMASTUser) this.actionHeuristic).setMASTStats(MASTStats);
    }
}
