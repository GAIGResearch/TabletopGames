package players.learners;

import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleLogisticLearner extends AbstractLearner {

    double[] coefficients;

    public static void main(String[] args) {
        File dir = new File(args[0]);
        if (!dir.isDirectory())
            throw new IllegalArgumentException(args[0] + " is not a directory");
        String[] files = Objects.requireNonNull(dir.list());
        files = Arrays.stream(files).map(file -> args[0] + File.separator + file).toArray(String[]::new);

        SimpleLogisticLearner learner = new SimpleLogisticLearner();
        learner.learnFrom(files);
        learner.writeToFile("test2.txt");
    }

    @Override
    public void learnFrom(String... files) {
        loadData(files);

        List<String> values = new ArrayList<>();
        values.add("0");
        values.add("1");
        attributes.add(new Attribute("Win", values));
        Instances dataInstances = new Instances("data", attributes, dataArray.length);
        for (int i = 0; i < dataArray.length; i++) {
            double[] record = dataArray[i];
            double[] XandY = new double[record.length + 1];
            System.arraycopy(record, 0, XandY, 0, record.length);
            XandY[record.length] = win[i][0];
            dataInstances.add(new DenseInstance(1.0, XandY));
        }
        dataInstances.setClassIndex(attributes.size() - 1);

        // BUGGER - this is only available as a classifier!
        Logistic regressor = new Logistic();
        regressor.setRidge(0.01);
        try {
            //      regressor.setDebug(true);
            regressor.buildClassifier(dataInstances);
            double[][] temp = regressor.coefficients();
            coefficients = new double[temp.length];
            for (int i = 0; i < temp.length; i++)
                coefficients[i] = temp[i][0];
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean writeToFile(String file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t")));
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
