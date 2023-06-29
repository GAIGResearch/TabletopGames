package players.rl;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class QWDSLinearApprox extends QWeightsDataStructure {

    private double[] qWeights;

    public QWDSLinearApprox(QWDSParams qwdsParams) {
        super(qwdsParams);
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
        // if (trainer != null)
        // qWeights = trainer.qWeights;
        // else
        qWeights = new double[playerParams.features.names().length];
    }

    @Override
    protected void parseQWeights(StateMap stateMap) {
        // TODO
        // qWeights =
        // Arrays.stream(qWeightStrings).mapToDouble(Double::parseDouble).toArray();
    }

    @Override
    protected StateMap qWeightsToStateMap() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'qWeightsToStateMap'");
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
