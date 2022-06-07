package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import java.io.IOException;

public class SVMStateHeuristic implements IStateHeuristic {

    IStateFeatureVector features;
    svm_model model;
    IStateHeuristic defaultHeuristic;

    public SVMStateHeuristic(String featureVectorClassName, String svmModelLocation, String defaultHeuristicClassName) {
        try {
            features = (IStateFeatureVector) Class.forName(featureVectorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem with Class : " + featureVectorClassName);
        }
        if (defaultHeuristicClassName.equals("")) {
            defaultHeuristic = new LeaderHeuristic();
        } else {
            try {
                defaultHeuristic = (IStateHeuristic) Class.forName(defaultHeuristicClassName).getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError("Problem with Class : " + defaultHeuristicClassName);
            }
        }
        loadModel(svmModelLocation);
    }
    public SVMStateHeuristic(String featureVectorClassName, String svmModelLocation) {
        this(featureVectorClassName, svmModelLocation, "");
    }

    public SVMStateHeuristic(IStateFeatureVector featureVector, String svmModelLocation, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        loadModel(svmModelLocation);
    }
    public SVMStateHeuristic(IStateFeatureVector featureVector, svm_model model, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        this.model = model;
    }

    private void loadModel(String svmModelLocation) {
        if (svmModelLocation.isEmpty())
            return;
        try {
            model = svm.svm_load_model(svmModelLocation);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem loading SVM model from file : " + svmModelLocation);
        }
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (model == null)
            return defaultHeuristic.evaluateState(state, playerId);
        double[] phi = features.featureVector(state, playerId);
        svm_node[] data = new svm_node[phi.length + 1];
        data[0] = new svm_node(); // bias
        data[0].index = 0;
        data[0].value = 1.0;
        for (int i = 0; i < phi.length; i++) {
            data[i + 1] = new svm_node();
            data[i + 1].index = i + 1;
            data[i + 1].value = phi[i];
        }
        double result = svm.svm_predict(model, data);
        return result;
    }
}
