package players.learners;


import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class OLSLearner extends ApacheLearner {

    double[] coefficients;

    public OLSLearner(double gamma, Target target) {
        super(gamma, target);
    }

    @Override
    void learnFromApacheData() {

        RFormula formula = new RFormula()
                .setFormula("target ~ " + String.join(" + ", descriptions))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(apacheData).transform(apacheData).select("features", "target");

        if (debug)
            training.show(10);

        LinearRegression lr = new LinearRegression()
                .setFitIntercept(true)
                .setMaxIter(10)
                .setRegParam(0.1)
                .setLabelCol("target")
                .setFeaturesCol("features");

        LinearRegressionModel lrModel = lr.fit(training);

        if (debug)
            System.out.println(lrModel.coefficients());

        coefficients = new double[descriptions.length + 1];
        coefficients[0] = lrModel.intercept();
        double[] coeffs = lrModel.coefficients().toArray();
        System.arraycopy(coeffs, 0, coefficients, 1, coeffs.length);
    }

    @Override
    public void writeToFile(String prefix) {
        String file = prefix + ".txt";
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("BIAS\t" + String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t")));
            writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String name() {
        return "OLS";
    }


}
