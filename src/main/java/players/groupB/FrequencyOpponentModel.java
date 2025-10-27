package players.groupB;

import java.util.HashMap;
import java.util.Map;

/**
 * FrequencyOpponentModel
 * Blueprint version:
 * - Tracks opponent action frequencies.
 * - Will later be used by the heuristic for predictive evaluation.
 */
public class FrequencyOpponentModel {

    // opponentId -> (action -> count)
    private Map<Integer, Map<String, Integer>> opponentFrequencies;

    public FrequencyOpponentModel() {
        opponentFrequencies = new HashMap<>();
    }

    /**
     * Record an opponent’s action.
     */
    public void updateModel(int opponentId, String action) {
        opponentFrequencies.putIfAbsent(opponentId, new HashMap<>());
        Map<String, Integer> actions = opponentFrequencies.get(opponentId);
        actions.put(action, actions.getOrDefault(action, 0) + 1);
    }

    /**
     * Get the relative frequency of a given action for a specific opponent.
     */
    public double getActionFrequency(int opponentId, String action) {
        if (!opponentFrequencies.containsKey(opponentId)) return 0.0;
        Map<String, Integer> actions = opponentFrequencies.get(opponentId);
        int total = actions.values().stream().mapToInt(Integer::intValue).sum();
        return total == 0 ? 0.0 : actions.getOrDefault(action, 0) / (double) total;
    }
}
