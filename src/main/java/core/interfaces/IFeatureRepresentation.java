package core.interfaces;

import core.CoreConstants;
import utilities.Distance;
import utilities.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface IFeatureRepresentation {

    /**
     * Create a double vector representation, where each element represents a feature in the game space by which
     * distance to another game state can be measured (in feature space), e.g.:
     *      - game score
     *      - player position
     *      - event counter value
     *      - round number
     * @param playerId - player observing the state
     * @return - int array, vector of features.
     */
    double[] getDistanceFeatures(int playerId);

    /**
     * Return all distance feature vectors which describe final game states, with associated result for the given player.
     * Includes a mapping from feature index (as given in getDistanceFeatures() method) to feature value, and
     * associated game result.
     * When features in distance feature vectors extracted from a state coincide with any of these values,
     * the game state is terminal.
     * @param playerId - player observing the state.
     * @return - map from terminal feature vector to game result.
     */
    HashMap<HashMap<Integer, Double>, CoreConstants.GameResult> getTerminalFeatures(int playerId);

    /**
     * Provide a numerical assessment of the current game state's distance to the other game state provided.
     * @param playerId - the player to calculate the score for.
     * @return double, distance to the other state provided.
     */
    default double getDistance(IFeatureRepresentation otherState, int playerId) {
        double[] features = getDistanceFeatures(playerId);
        double[] otherFeatures = otherState.getDistanceFeatures(playerId);
        return Distance.manhattan_distance(features, otherFeatures);
    }

    /**
     * Provide a numerical assessment of the current game state's distance to the other distance features provided.
     * @param playerId - the player to calculate the score for.
     * @return double, distance to the other features vector.
     */
    default double getDistance(double[] otherFeatures, int playerId) {
        double[] features = getDistanceFeatures(playerId);
        assert otherFeatures.length == features.length;
        return Distance.manhattan_distance(features, otherFeatures);
    }

    /**
     * Calculates the distances to all terminal states, returning a list of pairs (Distance, GameResult).
     * @param playerId - player observing the state.
     * @return - list of (distance, game result) pairs.
     */
    default ArrayList<Pair<Double, CoreConstants.GameResult>> getDistanceToTerminalStates(int playerId) {
        ArrayList<Pair<Double, CoreConstants.GameResult>> distances = new ArrayList<>();
        double[] features = getDistanceFeatures(playerId);

        HashMap<HashMap<Integer, Double>, CoreConstants.GameResult> terminalFeatures = getTerminalFeatures(playerId);
        for (Map.Entry<HashMap<Integer, Double>, CoreConstants.GameResult> e: terminalFeatures.entrySet()) {
            double[] otherFeatures = new double[features.length];
            for (Map.Entry<Integer, Double> m : e.getKey().entrySet()) {
                otherFeatures[m.getKey()] = m.getValue();
            }
            distances.add(new Pair<>(Distance.manhattan_distance(features, otherFeatures), e.getValue()));
        }
        return distances;
    }
}
