package players.rl;

import java.util.Arrays;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class LinearApproxQWDS extends QWeightsDataStructure {

    private double[] qWeights;

    @Override
    protected double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = params.features.featureVector(action, state, player.getPlayerID());
        double ret = 0;
        for (int i = 0; i < featureVector.length; i++)
            ret += qWeights[i] * featureVector[i];
        return ret;
    }

    @Override
    protected void initQWeights() {
        // if (trainer != null)
        // qWeights = trainer.qWeights;
        // else
        qWeights = new double[params.features.names().length];
    }

    @Override
    protected void parseQWeights(String[] qWeightStrings) {
        qWeights = Arrays.stream(qWeightStrings).mapToDouble(Double::parseDouble).toArray();
    }

    @Override
    protected String qWeightsToString() {
        String outputText = "";
        for (double q : qWeights)
            outputText += q + "\n";
        return outputText;
    }

    @Override
    protected void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    protected void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'qLearning'");
    }

}
