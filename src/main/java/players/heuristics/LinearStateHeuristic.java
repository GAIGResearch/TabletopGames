package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;

import java.io.*;
import java.util.Arrays;

public class LinearStateHeuristic implements IStateHeuristic {

    IStateFeatureVector features;
    double[] coefficients;

    public LinearStateHeuristic(String featureVectorClassName, String coefficientsFile) {
        try {
            features = (IStateFeatureVector) Class.forName(featureVectorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem with Class : " + featureVectorClassName);
        }
        loadModel(coefficientsFile);
    }

    public LinearStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile) {
        this.features = featureVector;
        loadModel(coefficientsFile);
    }
    private void loadModel(String coefficientsFile) {
        if (coefficientsFile.isEmpty()) {
            throw new AssertionError("No file for coefficients specified for LinearStateHeuristic");
        } else {
            File coeffFile = new File(coefficientsFile);
            try (BufferedReader br = new BufferedReader(new FileReader(coeffFile))) {
                String[] headers = br.readLine().split("\\t");
                if (!Arrays.equals(headers, features.names())) {
                    throw new AssertionError("Incompatible data in file " + coeffFile);
                }
                coefficients = Arrays.stream(br.readLine().split("\\t")).mapToDouble(Double::parseDouble).toArray();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new AssertionError("File not found : " + coeffFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError("Error accessing : " + coeffFile);
            }
        }
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        double[] phi = features.featureVector(state, playerId);
        double retValue = 0.0;
        for (int i = 0; i < phi.length; i++) {
            retValue += phi[i] * coefficients[i];
        }
        return retValue;
    }
}
