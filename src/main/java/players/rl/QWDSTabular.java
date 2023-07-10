package players.rl;

import java.util.Map;
import java.util.stream.Collectors;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer.RLType;

import java.util.Arrays;
import java.util.Comparator;

public class QWDSTabular extends QWeightsDataStructure {

    private Map<String, Double> qWeights;

    @Override
    protected void initializeEmpty() {
        qWeights = new StateMap();
    }

    @Override
    protected void applyGradient(RLPlayer player, AbstractGameState state, AbstractAction action, double delta) {
        double q = evaluateQ(player, state, action);
        qWeights.put(getStateId(player, state, action), q + delta);
    }

    @Override
    protected double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        return qWeights.getOrDefault(getStateId(player, state, action),
                playerParams.getTabularParams().unknownStateQValue);
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
        // Sort by value
        qWeightsStateMap = qWeightsStateMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                        StateMap::new));
        return qWeightsStateMap;
    }

    private String getStateId(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = playerParams.getFeatures().featureVector(action, state, player.getPlayerID());
        return Arrays.toString(featureVector).replaceAll(" ", "");
    }

    @Override
    public RLType getType() {
        return RLType.Tabular;
    }

}
