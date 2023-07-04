package players.rl;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer.RLType;

public class QWDSLinearApprox extends QWeightsDataStructure {

    private double[] qWeights;
    private String[] featureNames;

    public QWDSLinearApprox(String infileNameOrAbsPath) {
        super(infileNameOrAbsPath);
    }

    @Override
    protected double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action) {
        double[] featureVector = playerParams.features.featureVector(action, state, player.getPlayerID());
        double ret = 0;
        for (int i = 0; i < featureVector.length; i++)
            ret += qWeights[i] * featureVector[i];
        return ret;
    }

    @Override
    protected void initQWeightsEmpty() {
        featureNames = playerParams.features.names();
        qWeights = new double[featureNames.length];
    }

    @Override
    protected void parseQWeights(StateMap stateMap) {
        for (int i = 0; i < qWeights.length; i++)
            qWeights[i] = stateMap.get(featureNames[i]);
    }

    @Override
    protected StateMap qWeightsToStateMap() {
        return new StateMap() {
            {
                for (int i = 0; i < qWeights.length; i++)
                    put(featureNames[i], qWeights[i]);
            }
        };
    }

    @Override
    protected void applyGradient(RLPlayer player, AbstractGameState state, AbstractAction action, double delta) {
        double[] featureVector = playerParams.features.featureVector(action, state, player.getPlayerID());
        for (int i = 0; i < featureVector.length; i++)
            qWeights[i] += delta * featureVector[i];
    }

    @Override
    public RLType getType() {
        return RLType.LinearApprox;
    }

}
