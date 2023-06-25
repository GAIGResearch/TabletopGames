package players.rl.dataStructures;

import java.util.Arrays;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer;

public class LinearApproxQWDS extends QWeightsDataStructure {

    public double[] qWeights;

    @Override
    public double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = params.features.featureVector(action, state, player.getPlayerID());
        double ret = 0;
        for (int i = 0; i < featureVector.length; i++)
            ret += qWeights[i] * featureVector[i];
        return ret;
    }

    @Override
    public void initQWeights() {
        // if (trainer != null)
        // qWeights = trainer.qWeights;
        // else
        qWeights = new double[params.features.names().length];
    }

    @Override
    public void parseQWeightsTextFile(String[] qWeightStrings) {
        qWeights = Arrays.stream(qWeightStrings).mapToDouble(Double::parseDouble).toArray();
    }

    @Override
    public String qWeightsToString() {
        String outputText = "";
        for (double q : qWeights)
            outputText += q + "\n";
        return outputText;
    }

    @Override
    public void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'qLearning'");
    }

}
