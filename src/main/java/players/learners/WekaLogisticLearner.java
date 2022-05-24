package players.learners;

import weka.classifiers.functions.Logistic;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class WekaLogisticLearner extends AbstractLearner {

    double[] coefficients;
    int validationStart = 0;
    double validationProportion = 0.0;
    Logistic regressor = new Logistic();
    Instances dataInstances;

    public static void main(String[] args) {
        File dir = new File(args[0]);
        WekaLogisticLearner learner = new WekaLogisticLearner();

        String[] files = new String[1];
        if (dir.isDirectory()) {
            files = Arrays.stream(Objects.requireNonNull(dir.list()))
                    .map(file -> args[0] + File.separator + file)
                    .toArray(String[]::new);
        } else {
            files[0] = args[0];
        }

        learner.validationProportion = 0.20;
        learner.learnFrom(files);
        double[][] prediction = learner.predict();
        // this has one row per data item, and two columns for loss / win
        double sumError = 0.0;
        double sumErrorSquares = 0.0;
        for (int i = 0; i < prediction.length; i++) {
            double error = prediction[i][1] - learner.target[i][0];
            System.out.printf("Win: %.2f, Prediction: %.2f\n", learner.target[i][0], prediction[i][1]);
            sumError += Math.abs(error);
            sumErrorSquares += error * error;
        }
        System.out.printf("Final MAD: %.4f, MSE: %.4f\n", sumError / prediction.length, sumErrorSquares / prediction.length);

        learner.writeToFile("test2.txt");
    }

    @Override
    public void learnFrom(String... files) {
        loadData(files);
        validationStart = dataArray.length - (int) (dataArray.length * validationProportion);

        this.addNoise = true; // to avoid weka.Logistic silently discarding constants
        dataInstances = createInstances(false); // all data

        Instances trainingData = new Instances(dataInstances, 0, validationStart);

        regressor.setRidge(0.1);
        regressor.setDoNotStandardizeAttributes(true);
        try {
            //      regressor.setDebug(true);
            regressor.buildClassifier(trainingData);
     //       System.out.println(regressor);
            double[][] temp = regressor.coefficients();
            // this has the Bias term at the start of the array, where we want it
            coefficients = new double[temp.length];
            for (int i = 0; i < temp.length; i++)
                coefficients[i] = temp[i][0];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double[][] predict() {
        if (validationProportion == 0.0)
            throw new AssertionError("No validation data set");

        Instances validation = new Instances(dataInstances, validationStart, dataInstances.size() - validationStart);
        try {
            return regressor.distributionsForInstances(validation);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem in prediction");
        }
    }

    @Override
    public boolean writeToFile(String file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("BIAS\t" + String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.3g", d)).collect(Collectors.joining("\t")));
            writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String name() {
        return "Logistic";
    }
}
