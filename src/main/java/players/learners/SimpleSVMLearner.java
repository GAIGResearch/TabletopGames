package players.learners;

import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import libsvm.*;

public class SimpleSVMLearner extends AbstractLearner {

    svm_model model;

    public static void main(String[] args) {
        File dir = new File(args[0]);
        if (!dir.isDirectory())
            throw new IllegalArgumentException(args[0] + " is not a directory");
        String[] files = Objects.requireNonNull(dir.list());
        files = Arrays.stream(files).map(file -> args[0] + File.separator + file).toArray(String[]::new);

        SimpleSVMLearner learner = new SimpleSVMLearner();
        learner.learnFrom(files);
        learner.writeToFile("test_SVM.txt");
    }

    @Override
    public void learnFrom(String... files) {
        loadData(files);

        // SVM is at least available as a regressor

        svm_parameter params = new svm_parameter();
        params.gamma = 0.5;
        params.kernel_type = svm_parameter.LINEAR;
        params.svm_type = svm_parameter.EPSILON_SVR;
        params.C = 100;

        svm_problem data = new svm_problem();
        data.l = dataArray.length;
        data.y = new double[dataArray.length];
        data.x = new svm_node[dataArray.length][dataArray[0].length];

        for (int i = 0; i < dataArray.length; i++) {
            data.y[i] = win[i][0];
            for (int feature = 0; feature < dataArray[0].length; feature++) {
                data.x[i][feature] = new svm_node();
                data.x[i][feature].index = i;
                data.x[i][feature].value = dataArray[i][feature];
            }
        }

        model = svm.svm_train(data, params);

    }

    @Override
    public boolean writeToFile(String file) {
        try {
            svm.svm_save_model(file, model);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Error writing SVM model");
        }

        return true;
    }

    @Override
    public String name() {
        return "SVM";
    }
}
