package players.heuristics;

import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;

import java.io.*;
import java.util.Arrays;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public abstract class AbstractStateHeuristic implements IStateHeuristic {

    protected IStateFeatureVector features;
    protected double[] coefficients;
    protected IStateHeuristic defaultHeuristic = new LeaderHeuristic();

    public AbstractStateHeuristic(String featureVectorClassName, String coefficientsFile) {
        try {
            features = (IStateFeatureVector) Class.forName(featureVectorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem with Class : " + featureVectorClassName);
        }
        loadModel(coefficientsFile);
    }

    public AbstractStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile) {
        this.features = featureVector;
        loadModel(coefficientsFile);
    }
    private void loadModel(String coefficientsFile) {
        if (coefficientsFile.isEmpty()) {
            // in this case will default to the LeaderHeuristic
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

}
