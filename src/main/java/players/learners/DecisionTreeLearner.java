package players.learners;

import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.apache.spark.ml.regression.DecisionTreeRegressor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import players.heuristics.DecisionTreeActionHeuristic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class DecisionTreeLearner extends ApacheLearner {

    DecisionTreeRegressionModel drModel;
    int maxDepth;
    int minInstancesPerNode;
    double minInfoGain;
    double minWeightFractionPerNode;


    public DecisionTreeLearner(double gamma, Target target) {
        this(gamma, target, 10, 1, 0.00, 0.005);
    }

    public DecisionTreeLearner(double gamma, Target target, int maxDepth, int minInstancesPerNode, double minInfoGain, double minWeightFractionPerNode) {
        super(gamma, target);
        this.maxDepth = maxDepth;
        this.minInstancesPerNode = minInstancesPerNode;
        this.minInfoGain = minInfoGain;
        this.minWeightFractionPerNode = minWeightFractionPerNode;
    }


    @Override
    public void learnFromApacheData() {

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

    }

    @Override
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
