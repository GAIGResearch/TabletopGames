package players.mcts;

import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import players.simple.BoltzmannActionPlayer;
import utilities.Pair;

import java.util.List;
import java.util.Map;

public class MASTPlayer extends BoltzmannActionPlayer implements IMASTUser {


    public MASTPlayer(IActionKey actionKey, double temperature, double epsilon, long seed, double defaultValue) {
        super(new MASTActionHeuristic(null, actionKey, defaultValue), temperature, epsilon, seed);
    }
    @Override
    public void setStats(List<Map<Object, Pair<Integer, Double>>> MASTStats) {
        ((MASTActionHeuristic)this.actionHeuristic).MASTStatistics = MASTStats;
    }
}
