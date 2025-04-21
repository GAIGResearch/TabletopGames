package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;

public class DecisionTreeStateHeuristic extends AbstractDecisionTreeHeuristic implements IStateHeuristic {

    IStateFeatureVector stateFeatures;
    IStateHeuristic defaultHeuristic;
    public DecisionTreeStateHeuristic(IStateFeatureVector stateFeatures, String directory, IStateHeuristic defaultHeuristic) {
        super(directory);
        this.stateFeatures = stateFeatures;
        this.defaultHeuristic = defaultHeuristic;
    }
    public DecisionTreeStateHeuristic(IStateFeatureVector stateFeatures, DecisionTreeRegressionModel decisionTreeRegressionModel,
                                      IStateHeuristic defaultHeuristic) {
        super(decisionTreeRegressionModel);
        this.stateFeatures = stateFeatures;
        this.defaultHeuristic = defaultHeuristic;
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        // if terminal, we use the default heuristic
        if (defaultHeuristic != null && !state.isNotTerminal()) {
            return defaultHeuristic.evaluateState(state, playerId);
        }

        if (drModel == null) return 0;  // no model, no prediction (this is fine)

        // get the features for the state
        double[] features = this.stateFeatures.doubleVector(state, playerId);

        // return the prediction from the model
        return drModel.predict(Vectors.dense(features));
    }
}
