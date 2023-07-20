package players.mcts;

import utilities.Pair;

import java.util.List;
import java.util.Map;

public interface IMASTUser {

    void setStats(List<Map<Object, Pair<Integer, Double>>> MASTStats);
}
