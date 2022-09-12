package players.learners;

import weka.core.matrix.LinearRegression;
import weka.core.matrix.Matrix;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SimpleRegressionLearner extends AbstractLearner {

    double[] coefficients;

    @Override
    public void learnFrom(String... files) {
        loadData(files);
        // We now have all the data loaded, so can run regression
        Matrix X = new Matrix(dataArray);
        Matrix Y = new Matrix(target);

        LinearRegression regression = new LinearRegression(X, Y, 0.1);
        coefficients = regression.getCoefficients();
    }

    @Override
    public boolean writeToFile(String file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("BIAS\t" + String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t")));
            writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();            return false;
        }
        return true;
    }

    @Override
    public String name() {
        return "OLS";
    }

}
