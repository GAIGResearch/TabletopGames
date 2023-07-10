package players.rl;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer.RLType;
import players.rl.utils.TurnSAR;

public abstract class QWeightsDataStructure {

    public static final String qWeightsFolderName = "qWeights";

    // Alias for a sorted(!) hashmap between the state name
    // (players.rl.RLFeatureVector::names) and weight value
    protected class StateMap extends LinkedHashMap<String, Double> {
    }

    protected RLParams playerParams;
    protected RLTrainingParams trainingParams;

    // Ensures agent doesn't need to load the files between every game
    private boolean initialized = false;

    protected abstract void applyGradient(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    protected abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    protected abstract void parseQWeights(StateMap stateMap);

    protected abstract StateMap qWeightsToStateMap();

    protected void initialize(String gameName, RLParams playerParams) {
        if (initialized)
            return;
        this.playerParams = playerParams;
        initializeEmpty();
        if (playerParams.infileNameOrPath != null) {
            JsonNode weights = DataProcessor.readInputFile(gameName, playerParams.infileNameOrPath).get("Weights");
            readQWeightsFromInfile(weights);
        }
        initialized = true;
    }

    protected void initialize(RLTrainingParams trainingParams, RLParams playerParams) {
        this.trainingParams = trainingParams;
        initialize(trainingParams.gameName, playerParams);
    }

    protected abstract void initializeEmpty();

    private void readQWeightsFromInfile(JsonNode weights) {
        parseQWeights(new StateMap() {
            {
                weights.fields()
                        .forEachRemaining(e -> put(e.getKey(), e.getValue().asDouble()));
            }
        });
    }

    protected void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            // Turns at time t and t+1, respectively
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);

            double maxQ_s1a = t1.a == null ? 0
                    : t1.possibleActions.stream().mapToDouble(a -> evaluateQ(player, t1.s, a)).max().getAsDouble();

            // Q-Learning formula
            double q_s0a0 = evaluateQ(player, t0.s, t0.a);
            double delta = trainingParams.alpha * (t1.r + trainingParams.gamma * maxQ_s1a - q_s0a0);
            applyGradient(player, t0.s, t0.a, delta);
        }
    }

    public abstract RLType getType();

}