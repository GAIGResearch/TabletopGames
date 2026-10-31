package players.jsonBagPlayers;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

import java.util.*;


public class JSONBagHeuristic implements IStateHeuristic {

    Map<String, Integer> currentJSONBag;
    Set<String> filterSet;

    final List<Map<String, Double>> prototypes;

    public JSONBagHeuristic(List<? extends Map<String, ? extends Number>> prototypes_) {
        this(prototypes_, new ArrayList<>());
    }

    public JSONBagHeuristic(List<? extends Map<String, ? extends Number>> prototypes_, Collection<String> filterList_) {
        prototypes = new ArrayList<>(prototypes_.size());
        for (Map<String, ? extends Number> p : prototypes_) {
            Map<String, Double> m = new HashMap<>();
            for (Map.Entry<String, ? extends Number> e : p.entrySet()) {
                m.put(e.getKey(), e.getValue().doubleValue());
            }
            prototypes.add(Utils.normaliseDistribution(m));
        }
        filterSet = Set.copyOf(filterList_);
        currentJSONBag = new HashMap<String, Integer>();
    }


    public void setFilterSet(Collection<String> filterSet_) {
        filterSet = Set.copyOf(filterSet_);
    }

    public void updateJSONBag(AbstractGameState gs) {
        Map<String, Integer> tokenizedGameState = Tokenizer.tokenize(gs, filterSet, true);
//        Tokenizer.filter(tokenizedGameState, filterSet,true);
        Tokenizer.merge(currentJSONBag, tokenizedGameState);
    }

    public void resetCurrentBag(AbstractGameState gs) {
        currentJSONBag = new HashMap<String, Integer>();
        updateJSONBag(gs);
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getNPlayers() != prototypes.size()) {
            throw new RuntimeException("PLAYER COUNT AND PROTOTYPE COUNT MISMATCHED: " + gs.getNPlayers() + " vs. " + prototypes.size());
        }
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return 0;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return 1.0;
        Map<String, Integer> newJSONBag = new HashMap<>(currentJSONBag);
        Map<String, Integer> newGameStateTokenized = Tokenizer.tokenize(gs, filterSet, true);
//        Tokenizer.filter(newGameStateTokenized, filterSet,true);
        Tokenizer.merge(newJSONBag, newGameStateTokenized);

        double[] distances = new double[prototypes.size()];
        for (int i = 0; i < prototypes.size(); i++) {
            distances[i] = Utils.jensenShannonDistance(prototypes.get(i), newJSONBag);
        }
//        double[] values = invertNormalized(distances);
        double[] values = subtractNormalized(distances);
//        double[] values = negative(distances);

//        values = softmax(values, 0.5);
        return values[playerId];
//        return 1.0 - distances[playerId];
    }

    private double[] invertNormalized(double[] distancesArray) {
        double[] ret = new double[distancesArray.length];
        double sum = 0.0;
        for (int i = 0; i < distancesArray.length; i++) {
            ret[i] = 1.0 / distancesArray[i];
            sum += ret[i];
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] /= sum;
        }
        return ret;
    }

    private double[] subtractNormalized(double[] distancesArray) {
        double[] ret = new double[distancesArray.length];
        double sum = 0.0;
        for (int i = 0; i < distancesArray.length; i++) {
            ret[i] = 1.0 - distancesArray[i];
            sum += ret[i];
        }
        if (!(sum > 0.0)) {
            throw new IllegalStateException("JSON-Bag value function is degenerate: every prototype is at maximal "
                    + "Jensen-Shannon distance from the current bag (distances " + Arrays.toString(distancesArray)
                    + "), so no player can be ranked. The trajectory shares no tokens with any prototype - "
                    + "check the prototypes and the token filter.");
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] /= sum;
        }
        return ret;
    }

    private double[] negative(double[] distances) {
        double[] ret = new double[distances.length];
        for (int i = 0; i < distances.length; i++) {
            ret[i] = - distances[i];
        }
        return ret;
    }

    private double[] softmax(double[] values, double temperature) {
        double[] ret = new double[values.length];
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            if (v > max) max = v;
        }
        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            ret[i] = Math.exp((values[i] - max) / temperature);
            sum += ret[i];
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] /= sum;
        }
        return ret;
    }

    public JSONBagHeuristic copy() {
        JSONBagHeuristic copy = new JSONBagHeuristic(prototypes, new ArrayList<>(filterSet));
        copy.currentJSONBag = new HashMap<>(currentJSONBag);
        return copy;
    }

    @Override
    public double minValue() {
        return 0;
    }
}
