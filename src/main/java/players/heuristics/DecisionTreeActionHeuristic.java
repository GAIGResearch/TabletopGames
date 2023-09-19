package players.heuristics;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateFeatureVector;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.apache.spark.sql.SparkSession;

import java.util.List;

public class DecisionTreeActionHeuristic implements IActionHeuristic {

    static SparkSession spark = SparkSession
            .builder()
            .appName("Java Spark SQL basic example")
            //     .config("spark.driver.memory", "1g")
            .master("local").getOrCreate();
    static {
        // And the hack to get this to work on Windows (without the Winutils.exe and hadoop.dll nightmare)
        spark.sparkContext().hadoopConfiguration().setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);
    }

    DecisionTreeRegressionModel drModel;
    IStateFeatureVector stateFeatures;
    IActionFeatureVector actionFeatures;
    public DecisionTreeActionHeuristic(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures, String directory) {
        // load in the Decision Tree model from the directory
        if (directory == null || directory.isEmpty()) {
            System.out.println("No directory specified for Decision Tree model");
            return;  // this is fine; we just use a null value
        }
        drModel = DecisionTreeRegressionModel.load(directory);
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
    }
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state) {
        if (drModel == null) return 0;  // no model, no prediction (this is fine
        // get the features for the state and action
        int playerId = state.getCurrentPlayer();
        double[] stateFeatures = this.stateFeatures.featureVector(state, playerId);
        double[] actionFeatures = this.actionFeatures.featureVector(action, state, playerId);
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
        double[] stateFeatures = this.stateFeatures.featureVector(state, playerId);
        // Then we get the action features for each action
        double[][] actionFeatures = new double[actions.size()][];
        for (int i = 0; i < actions.size(); i++) {
            actionFeatures[i] = this.actionFeatures.featureVector(actions.get(i), state, playerId);
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

    public static String prettifyDecisionTreeDescription(DecisionTreeRegressionModel model, String[] featureNames) {
        // the debug string of model contains labels of the form 'feature nn', where nn is the index of the feature
        // We want to replace these with the actual feature names
        // we go in reverse to stop replacing 'feature 10' with 'nameOfFeature0' etc.
        String debugString = model.toDebugString();
        for (int i = featureNames.length-1; i >= 0; i--) {
            debugString = debugString.replace("feature " + i, featureNames[i]);
        }
        return debugString;
    }

}
