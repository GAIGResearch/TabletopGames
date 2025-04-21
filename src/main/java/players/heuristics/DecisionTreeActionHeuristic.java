package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateFeatureVector;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;

import java.util.List;

public class DecisionTreeActionHeuristic extends AbstractDecisionTreeHeuristic implements IActionHeuristic {

    IStateFeatureVector stateFeatures;
    IActionFeatureVector actionFeatures;
    public DecisionTreeActionHeuristic(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures, String directory) {
        super(directory);
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
    }
    public DecisionTreeActionHeuristic(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures, DecisionTreeRegressionModel drModel) {
        super(drModel);
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
    }
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        if (drModel == null) return 0;  // no model, no prediction (this is fine
        // get the features for the state and action
        int playerId = state.getCurrentPlayer();
        double[] stateFeatures = this.stateFeatures.doubleVector(state, playerId);
        double[] actionFeatures = this.actionFeatures.doubleVector(action, state, playerId);
        // combine the features
        double[] features = new double[stateFeatures.length + actionFeatures.length];
        System.arraycopy(stateFeatures, 0, features, 0, stateFeatures.length);
        System.arraycopy(actionFeatures, 0, features, stateFeatures.length, actionFeatures.length);
        // return the prediction from the model

        return drModel.predict(Vectors.dense(features));
    }

    @Override
    public double[] evaluateAllActions(List<AbstractAction> actions, AbstractGameState state) {
        if (drModel == null) return new double[actions.size()];  // no model, no prediction (this is fine)
        // First we get the state features once
        int playerId = state.getCurrentPlayer();
        double[] stateFeatures = this.stateFeatures.doubleVector(state, playerId);
        // Then we get the action features for each action
        double[][] actionFeatures = new double[actions.size()][];
        for (int i = 0; i < actions.size(); i++) {
            actionFeatures[i] = this.actionFeatures.doubleVector(actions.get(i), state, playerId);
        }
        // Then we combine the features
        double[][] features = new double[actions.size()][stateFeatures.length + actionFeatures[0].length];
        for (int i = 0; i < actions.size(); i++) {
            System.arraycopy(stateFeatures, 0, features[i], 0, stateFeatures.length);
            System.arraycopy(actionFeatures[i], 0, features[i], stateFeatures.length, actionFeatures[i].length);
        }
        // Then we return the predictions from the model
        double[] predictions = new double[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            predictions[i] = drModel.predict(Vectors.dense(features[i]));
        }
        return predictions;
    }


}
