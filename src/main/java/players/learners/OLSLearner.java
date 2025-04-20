package players.learners;


import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.FileWriter;

public class OLSLearner extends ApacheLearner {

    double[] coefficients;
    double elasticNetParam = 0.8;
    double regParam = 0.1;

    public OLSLearner(Target target) {
        super(1.0, target);
    }

    public OLSLearner(double gamma, double elasticNetParam, double regParam, Target target) {
        super(gamma, target);
        this.elasticNetParam = elasticNetParam;
        this.regParam = regParam;
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
                .setRegParam(regParam)
                .setElasticNetParam(elasticNetParam)
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
        writeToFile(prefix, descriptions, coefficients);
    }

    public static void writeToFile(String prefix, String[] descriptions, double[] coefficients) {
        // we don't go via JSONUtils because we want to keep the order of coefficients in the output file

        // remove the current suffix (if one exists)
        if (prefix.contains(".")) {
            prefix = prefix.substring(0, prefix.lastIndexOf('.'));
        }
        String file = prefix + "_coeffsOnly.json";
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("{\n");
            writer.write("\t\"BIAS\": " + String.format("%.3g", coefficients[0]) + ",\n");
            for (int i = 1; i < coefficients.length; i++) {
                if (Math.abs(coefficients[i]) < 0.000001) continue; // skip zero coefficients
                writer.write("\t\"" + descriptions[i - 1] + "\": " + String.format("%.3g", coefficients[i]));
                if (i < coefficients.length - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("}\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // then write the full class


    }

    @Override
    public String name() {
        return "OLS";
    }


}
