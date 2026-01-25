package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.GeneralizedLinearRegression;
import org.apache.spark.ml.regression.GeneralizedLinearRegressionModel;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import players.heuristics.LogisticActionHeuristic;
import players.heuristics.LogisticStateHeuristic;
import players.heuristics.OrdinalPosition;
import players.heuristics.WinOnlyHeuristic;

import java.io.FileWriter;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;

public class LogisticLearner extends ApacheLearner {

    double[] coefficients;
    double regParam = 0.1;

    public LogisticLearner() {
        super();
    }

    public LogisticLearner(double gamma, double regularisation, Target target) {
        this(gamma, regularisation, target, null, null);
    }

    public LogisticLearner(Target target, IStateFeatureVector stateFeatureVector) {
        super(1.0, target, stateFeatureVector);
    }

    public LogisticLearner(Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        super(1.0, target, stateFeatureVector, actionFeatureVector);
    }

    public LogisticLearner(double gamma, double regParam, Target target, IStateFeatureVector stateFeatureVector) {
        super(gamma, target, stateFeatureVector);
        this.regParam = regParam;
    }

    public LogisticLearner(double gamma, double regParam, Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        super(gamma, target, stateFeatureVector, actionFeatureVector);
        this.regParam = regParam;
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

        GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                .setFitIntercept(true)
                .setMaxIter(10)
                .setFamily("Binomial")
                .setLink("Logit")
                .setRegParam(regParam)
                .setLabelCol("target")
                .setFeaturesCol("features");

        GeneralizedLinearRegressionModel lrModel = lr.fit(training);

        if (debug)
            System.out.println(lrModel.coefficients());

        if (this.actionFeatureVector == null) {
            LogisticStateHeuristic retValue = new LogisticStateHeuristic(stateFeatureVector, coefficients, new WinOnlyHeuristic());
            retValue.setModel(lrModel);
            return retValue;
        } else {
            LogisticActionHeuristic retValue = new LogisticActionHeuristic(actionFeatureVector, stateFeatureVector, coefficients);
            retValue.setModel(lrModel);
            return retValue;
        }
    }

    @Override
    public String name() {
        return "Logistic";
    }

}
