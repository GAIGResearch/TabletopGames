package players.groupB;

import java.util.HashMap;
import java.util.Map;

/**
 * FrequencyOpponentModel
 * Tracks how frequently each opponent chooses specific actions.
 */
public class FrequencyOpponentModel {

    // opponentID -> (actionName -> count)
    private Map<Integer, Map<String, Integer>> opponentFrequencies;

    public FrequencyOpponentModel() {
        opponentFrequencies = new HashMap<>();
    }

    /**
     * Called every time an opponent makes a move.
     */
    public void updateModel(int opponentId, String action) {
        opponentFrequencies.putIfAbsent(opponentId, new HashMap<>());
        Map<String, Integer> freqMap = opponentFrequencies.get(opponentId);
        freqMap.put(action, freqMap.getOrDefault(action, 0) + 1);
    }

    /**
     * Returns how likely an opponent is to choose this action.
     * Used by the heuristic to predict threats.
     */
    public double getActionProbability(int opponentId, String action) {
        if (!opponentFrequencies.containsKey(opponentId)) return 0.0;
        Map<String, Integer> freqMap = opponentFrequencies.get(opponentId);
        int total = freqMap.values().stream().mapToInt(Integer::intValue).sum();
        return total == 0 ? 0.0 : freqMap.getOrDefault(action, 0) / (double) total;
    }

    /**
     * returns how aggressive or risky the opponent seems.
     */
    public double getAggressivenessScore(int opponentId) {
        if (!opponentFrequencies.containsKey(opponentId)) return 0.0;
        // placeholder heuristic: number of actions recorded = engagement level
        return opponentFrequencies.get(opponentId).size();
    }
}
