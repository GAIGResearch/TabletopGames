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

    public SVMStateHeuristic(String featureVectorClassName, String svmModelLocation) {
        try {
            features = (IStateFeatureVector) Class.forName(featureVectorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem with Class : " + featureVectorClassName);
        }
        loadModel(svmModelLocation);
    }

    public SVMStateHeuristic(IStateFeatureVector featureVector, String svmModelLocation) {
        this.features = featureVector;
        loadModel(svmModelLocation);
    }
    private void loadModel(String svmModelLocation) {
        try {
            model = svm.svm_load_model(svmModelLocation);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem loading SVM model from file : " + svmModelLocation);
        }
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        double[] phi = features.featureVector(state, playerId);
        svm_node[] data = new svm_node[phi.length];
        for (int i=0; i < phi.length; i++) {
            data[i] = new svm_node();
            data[i].index = i;
            data[i].value = phi[i];
        }
        return svm.svm_predict(model, data);
    }
}
