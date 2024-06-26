package players.learners;

import libsvm.*;
import utilities.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class SVMLearner extends AbstractLearner {

    svm_model model;
    svm_parameter params = new svm_parameter();

    public SVMLearner() {
        params.gamma = 5.0;
        params.kernel_type = svm_parameter.RBF;
        params.degree = 2;
        params.svm_type = svm_parameter.EPSILON_SVR;
        params.nu = 0.60;
        params.C = 1.0;
        params.eps = 0.01;
        params.p = 0.003;
        params.shrinking = 1;
    }
    public SVMLearner(svm_parameter params) {
        this.params = params;
    }

    public static void main(String[] args) {
        File dir = new File(args[0]);
        if (!dir.isDirectory())
            throw new IllegalArgumentException(args[0] + " is not a directory");
        String[] files = Objects.requireNonNull(dir.list());
        files = Arrays.stream(files).map(file -> args[0] + File.separator + file).toArray(String[]::new);

        SVMLearner learner = new SVMLearner();
        learner.learnFrom(files);
        learner.writeToFile("test_SVM_NU_06.txt");
    }

    @Override
    public void learnFrom(String... files) {
        loadData(files);

        // SVM is at least available as a regressor (Apache only supports SVMs for classification)
        // unlike the Weka Logistic implementation, here we do not do any messing about with a bias term
        // which is just passed through (it is the first element in dataArray)
        svm_problem data = new svm_problem();
        data.l = dataArray.length;
        data.y = new double[dataArray.length];
        data.x = new svm_node[dataArray.length][dataArray[0].length];

        for (int i = 0; i < dataArray.length; i++) {
            data.y[i] = target[i][0];
            for (int feature = 0; feature < dataArray[0].length; feature++) {
                data.x[i][feature] = new svm_node();
                data.x[i][feature].index = i;
                data.x[i][feature].value = dataArray[i][feature];
            }
        }

        model = svm.svm_train(data, params);
    }

    public Pair<Double, Double> validate(String... files) {
        loadData(files);

        double medianDiff = 0.0;
        double squaredDiff = 0.0;
        for (int i = 0; i < dataArray.length; i++) {
            svm_node[] data = new svm_node[dataArray[0].length];
            for (int feature = 0; feature < dataArray[0].length; feature++) {
                data[feature] = new svm_node();
                data[feature].index = i;
                data[feature].value = dataArray[i][feature];
            }

            double prediction = svm.svm_predict(model, data);
            medianDiff += Math.abs(prediction - target[i][0]);
            squaredDiff += (prediction - target[i][0])  * (prediction - target[i][0]);
        }

        return new Pair<>(medianDiff / dataArray.length, squaredDiff / dataArray.length);
    }



    @Override
    public void writeToFile(String prefix) {
        String file = prefix + ".txt";
        try {
            svm.svm_save_model(file, model);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Error writing SVM model");
        }

    }

    @Override
    public String name() {
        return "SVM";
    }

    public svm_parameter getParams() {return params;}
}
