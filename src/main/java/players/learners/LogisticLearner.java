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

    public static void main(String[] args) {

        Dataset<Row> df = spark.read()
                .option("delimiter", "\t")
                .option("header", "true")
                .option("inferSchema", "true")
                .csv("Apache_0.data");

        df.show(10);

        String[] regressors = new String[]{"POINT_ADVANTAGE", "POINTS", "THREE_BOXES", "TWO_BOXES"};


        //"GOLD_IN_DECK", "PROVINCE_IN_DECK", "ESTATE_IN_DECK", "DUCHY_IN_DECK", "TR_H", "AC_LEFT", "BUY_LEFT", "TOT_CRDS", "ROUND11", "OUR_TURN"};

        // headers are not case-sensitive, so ORDINAL7 is the current position, and Ordinal119 is the final achieved position
        try {
            df.createTempView("data");
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        // for 4 players
        df = spark.sql(String.format("select %s, (1 - (Ordinal13 - 1) / 3) as Ordinal From data", String.join(", ", regressors)));

        df.show(10);

        RFormula formula = new RFormula()
                .setFormula("Ordinal ~ " + String.join(" + ", regressors))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(df).transform(df).select("features", "target");

        training.show(10);

        GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                .setMaxIter(10)
                .setFamily("Binomial")
                .setLink("Logit")
                .setRegParam(0.01)
                .setLabelCol("target")
                .setFeaturesCol("features");

        GeneralizedLinearRegressionModel lrModel = lr.fit(training);

        System.out.println(lrModel.coefficients());


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

        coefficients = new double[descriptions.length + 1];
        coefficients[0] = lrModel.intercept();
        double[] coeffs = lrModel.coefficients().toArray();
        System.arraycopy(coeffs, 0, coefficients, 1, coeffs.length);

        if (this.actionFeatureVector == null) {
            return new LogisticStateHeuristic(stateFeatureVector, coefficients, new WinOnlyHeuristic());
        } else {
            return new LogisticActionHeuristic(actionFeatureVector, stateFeatureVector, coefficients);
        }
    }

    @Override
    public void writeToFile(String prefix) {
        OLSLearner.writeToFile(prefix, descriptions, coefficients, this);
    }

    @Override
    public String name() {
        return "Logistic";
    }

}
