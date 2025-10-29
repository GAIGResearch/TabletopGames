package players.groupB;

import core.AbstractGameState;

import java.util.HashMap;
import java.util.Map;

/**
 * FrequencyOpponentModel
 * ----------------------
 * Simple statistical model tracking how frequently each opponent
 * chooses specific actions (by name). This allows the heuristic
 * to estimate "pressure" on certain cards or strategies.
 *
 * The model is game-agnostic and safe to call in any TAG environment.
 */
public class FrequencyOpponentModel {

    // Mapping: opponentID -> (actionName -> count)
    private final Map<Integer, Map<String, Integer>> opponentFrequencies;

    /** Default constructor initialises the map */
    public FrequencyOpponentModel() {
        this.opponentFrequencies = new HashMap<>();
    }

    // ---------------------------------------------------------------------
    // Core model methods
    // ---------------------------------------------------------------------

    /**
     * Update the model whenever an opponent performs an action.
     * @param opponentId ID of the opponent
     * @param actionName Action performed (string identifier)
     */
    public void updateModel(int opponentId, String actionName) {
        opponentFrequencies.putIfAbsent(opponentId, new HashMap<>());
        Map<String, Integer> freqMap = opponentFrequencies.get(opponentId);
        freqMap.put(actionName, freqMap.getOrDefault(actionName, 0) + 1);
    }

    /**
     * Get the probability that an opponent will take a given action.
     * Computed as (count of action) / (total actions observed).
     */
    public double getActionProbability(int opponentId, String actionName) {
        if (!opponentFrequencies.containsKey(opponentId)) return 0.0;
        Map<String, Integer> freqMap = opponentFrequencies.get(opponentId);
        int total = freqMap.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        return freqMap.getOrDefault(actionName, 0) / (double) total;
    }

    /**
     * A coarse metric for how "aggressive" an opponent seems.
     * Defined as the number of distinct actions they've taken.
     * Higher = more exploratory / competitive.
     */
    public double getAggressivenessScore(int opponentId) {
        if (!opponentFrequencies.containsKey(opponentId)) return 0.0;
        return opponentFrequencies.get(opponentId).size();
    }

    // ---------------------------------------------------------------------
    // Optional integration hook
    // ---------------------------------------------------------------------

    /**
     * Optional hook for games that expose history in AbstractGameState.
     * Can be used to automatically infer recent opponent actions.
     * (Safe no-op by default to ensure cross-game compatibility.)
     */
    public void updateFromGameState(AbstractGameState gameState) {
        // Example extension:
        // if (gameState.getHistory().size() > 0) {
        //     AbstractAction lastAction = gameState.getHistory().getLastAction();
        //     int lastPlayer = gameState.getHistory().getLastPlayer();
        //     updateModel(lastPlayer, lastAction.toString());
        // }
    }
}