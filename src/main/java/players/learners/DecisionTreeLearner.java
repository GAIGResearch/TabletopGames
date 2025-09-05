package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.apache.spark.ml.regression.DecisionTreeRegressor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import players.heuristics.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class DecisionTreeLearner extends ApacheLearner {

    DecisionTreeRegressionModel drModel;
    int maxDepth;
    int minInstancesPerNode;
    double minInfoGain;
    double minWeightFractionPerNode;


    public DecisionTreeLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector) {
        this(gamma, target, 10, 1, 0.00, 0.005, stateFeatureVector);
    }
    public DecisionTreeLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        this(gamma, target, 10, 1, 0.00, 0.005, stateFeatureVector, actionFeatureVector);
    }

    public DecisionTreeLearner(double gamma, Target target, int maxDepth, int minInstancesPerNode,
                               double minInfoGain, double minWeightFractionPerNode,
                               IStateFeatureVector stateFeatureVector) {
        super(gamma, target, stateFeatureVector);
        this.maxDepth = maxDepth;
        this.minInstancesPerNode = minInstancesPerNode;
        this.minInfoGain = minInfoGain;
        this.minWeightFractionPerNode = minWeightFractionPerNode;
    }
    public DecisionTreeLearner(double gamma, Target target, int maxDepth, int minInstancesPerNode,
                               double minInfoGain, double minWeightFractionPerNode,
                               IStateFeatureVector stateFeatureVector,
                               IActionFeatureVector actionFeatureVector) {
        super(gamma, target, stateFeatureVector, actionFeatureVector);
        this.maxDepth = maxDepth;
        this.minInstancesPerNode = minInstancesPerNode;
        this.minInfoGain = minInfoGain;
        this.minWeightFractionPerNode = minWeightFractionPerNode;
    }


    @Override
    public Object learnFromApacheData() {

        RFormula formula = new RFormula()
                .setFormula("target ~ " + String.join(" + ", descriptions))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(apacheData).transform(apacheData).select("features", "target");

        if (debug)
            training.show(10);


// Train a DecisionTree model.
        DecisionTreeRegressor dr = new DecisionTreeRegressor()
                .setLabelCol("target")
                .setFeaturesCol("features")
                .setMaxDepth(maxDepth)
                .setMinInstancesPerNode(minInstancesPerNode)
                .setMinInfoGain(minInfoGain)
                .setMinWeightFractionPerNode(minWeightFractionPerNode);

        drModel = dr.fit(training);

        if (debug)
            System.out.println(DecisionTreeActionHeuristic.prettifyDecisionTreeDescription(drModel, descriptions));

        if (this.actionFeatureVector == null) {
            return new DecisionTreeStateHeuristic(stateFeatureVector, drModel, switch (targetType) {
                case ORDINAL, ORD_MEAN, ORD_SCALE, ORD_MEAN_SCALE -> new OrdinalPosition();
                case SCORE -> new PureScoreHeuristic();
                case SCORE_DELTA -> new LeaderHeuristic();
                default -> new WinOnlyHeuristic();
            });
        } else {
            return new DecisionTreeActionHeuristic(stateFeatureVector, actionFeatureVector, drModel);
        }
    }

    public void writeToFile(String file) {
        try {
            drModel.write().overwrite().save(file);
            BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(file + File.separator + "Description.txt"));
            writer.write(DecisionTreeActionHeuristic.prettifyDecisionTreeDescription(drModel, descriptions));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save decision tree model");
            drModel.toDebugString();
        }

    }

    @Override
    public String name() {
        return "DecisionTree";
    }

}
