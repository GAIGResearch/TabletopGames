package players.rl.dataStructures;

import java.util.Map;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TabularQWDS extends QWeightsDataStructure {

    public Map<String, Double> qWeights;

    @Override
    public void initQWeights() {
        qWeights = new HashMap<String, Double>();
    }

    @Override
    public void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q) {
        qWeights.put(getStateId(player, state, action), q);
    }

    @Override
    public double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        return qWeights.getOrDefault(getStateId(player, state, action), 0.5);
    }

    @Override
    public void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1) {
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
    public void parseQWeightsTextFile(String[] qWeightStrings) {
        Arrays.stream(qWeightStrings).forEach(s -> {
            String[] entry = s.split(":");
            qWeights.put(entry[0], Double.parseDouble(entry[1]));
        });
    }

    @Override
    public String qWeightsToString() {
        List<String> outputs = new LinkedList<String>();
        for (String stateId : qWeights.keySet())
            if (qWeights.containsKey(stateId))
                outputs.add(stateId + ":" + qWeights.get(stateId));
        outputs.sort(Comparator.comparingDouble((String s) -> Double.parseDouble(s.split(":")[1])).reversed());
        String outputText = "";
        for (String s : outputs)
            outputText += s + "\n";
        return outputText;
    }

    private String getStateId(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = params.features.featureVector(action, state, player.getPlayerID());
        return Arrays.toString(featureVector).replaceAll(" ", "");
    }

}
