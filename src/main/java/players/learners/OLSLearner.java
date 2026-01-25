package players.learners;


import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.GeneralizedLinearRegression;
import org.apache.spark.ml.regression.GeneralizedLinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import players.heuristics.*;

public class OLSLearner extends ApacheLearner {

    double[] coefficients;
    double regParam = 0.1;

    public OLSLearner() {
        super();
    }
    public OLSLearner(double gamma, double regParam, Target target) {
        this(gamma, regParam, target, null, null);
    }

    public OLSLearner(Target target, IStateFeatureVector stateFeatureVector) {
        super(1.0, target, stateFeatureVector);
    }

    public OLSLearner(Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        super(1.0, target, stateFeatureVector, actionFeatureVector);
    }

    public OLSLearner(double gamma, double regParam, Target target,
                      IStateFeatureVector stateFeatureVector) {
        super(gamma, target, stateFeatureVector);
        this.regParam = regParam;
    }

    public OLSLearner(double gamma, double regParam, Target target,
                      IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        super(gamma, target, stateFeatureVector, actionFeatureVector);
        this.regParam = regParam;
    }

    @Override
    Object learnFromApacheData() {

        RFormula formula = new RFormula()
                .setFormula("target ~ " + String.join(" + ", descriptions))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(apacheData).transform(apacheData).select("features", "target");

        if (debug)
            training.show(10);

//        LinearRegression lr = new LinearRegression()
//                .setFitIntercept(true)
//                .setMaxIter(10)
//                .setRegParam(regParam)
//                .setElasticNetParam(elasticNetParam)
//                .setLabelCol("target")
//                .setFeaturesCol("features");
//
//        LinearRegressionModel lrModel = lr.fit(training);
        GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                .setFitIntercept(true)
                .setMaxIter(10)
                .setFamily("gaussian")
                .setLink("identity")
                .setRegParam(regParam)
                .setLabelCol("target")
                .setFeaturesCol("features");

        GeneralizedLinearRegressionModel lrModel = lr.fit(training);

        if (debug)
            System.out.println(lrModel.coefficients());

        if (this.actionFeatureVector == null) {
            // return the learned OLS heuristic
            LinearStateHeuristic retValue = new LinearStateHeuristic(stateFeatureVector, coefficients,
                    switch (targetType) {
                        case ORDINAL, ORD_MEAN, ORD_SCALE, ORD_MEAN_SCALE -> new OrdinalPosition();
                        case SCORE -> new PureScoreHeuristic();
                        case SCORE_DELTA -> new LeaderHeuristic();
                        default -> new WinOnlyHeuristic();
                    });
            retValue.setModel(lrModel);
            return retValue;
        } else {
            // return the learned OLS heuristic
            LinearActionHeuristic retValue = new LinearActionHeuristic(actionFeatureVector, stateFeatureVector, coefficients);
            retValue.setModel(lrModel);
            return retValue;
        }
    }

    @Override
    public String name() {
        return "OLS";
    }


}
