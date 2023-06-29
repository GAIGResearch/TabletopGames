package players.rl;

import java.util.Map;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Arrays;

public class QWDSTabular extends QWeightsDataStructure {

    private Map<String, Double> qWeights;

    @Override
    protected void initQWeightsEmpty() {
        qWeights = new StateMap();
    }

    @Override
    protected void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q) {
        qWeights.put(getStateId(player, state, action), q);
    }

    @Override
    protected double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        return qWeights.getOrDefault(getStateId(player, state, action), params.tabular.unknownStateQValue);
    }

    @Override
    protected void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1) {
        AbstractGameState s0 = t0.s.copy();
        AbstractGameState s1 = t1.s;

        // TODO more sophisticated reward?
        double maxQ_s1a = t1.a == null
                ? 0
                : t1.possibleActions.stream().mapToDouble(a -> evaluateQ(player, s1, a)).max()
                        .getAsDouble();

        double q_s0a0 = evaluateQ(player, s0, t0.a);
        // Q-Learning formula
        q_s0a0 = q_s0a0 + trainingParams.alpha * (t1.r + trainingParams.gamma * maxQ_s1a - q_s0a0);
        add(player, s0, t0.a, q_s0a0);
    }

    @Override
    protected void parseQWeights(StateMap stateMap) {
        qWeights = stateMap;
    }

    @Override
    protected StateMap qWeightsToStateMap() {
        StateMap qWeightsStateMap = new StateMap();
        for (String stateId : qWeights.keySet())
            qWeightsStateMap.put(stateId, qWeights.get(stateId));
        // qWeightsStateMap.sort(Comparator.comparingDouble((String s) ->
        // Double.parseDouble(s.split(":")[1])).reversed());
        return qWeightsStateMap;
    }

    private String getStateId(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = params.features.featureVector(action, state, player.getPlayerID());
        return Arrays.toString(featureVector).replaceAll(" ", "");
    }

}
