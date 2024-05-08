package players.heuristics;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateFeatureVector;

import java.io.*;
import java.util.Arrays;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public abstract class GLMActionHeuristic implements IActionHeuristic {

    protected IStateFeatureVector features;
    protected IActionFeatureVector actionFeatures;
    protected double[] coefficients;

    /**
     * The coefficientsFile is a tab separated file with the first line being the names of the features
     * and the second line being the coefficients.
     * <p>
     * The convention required is that the State coefficients are first, followed by the Action coefficients.
     *
     * @param featureVector
     * @param actionFeatureVector
     * @param coefficientsFile
     */
    public GLMActionHeuristic(IStateFeatureVector featureVector, IActionFeatureVector actionFeatureVector, String coefficientsFile) {
        this.features = featureVector;
        this.actionFeatures = actionFeatureVector;
        loadModel(coefficientsFile);
    }

    private void loadModel(String coefficientsFile) {
        String[] expectedHeaders = new String[features.names().length + actionFeatures.names().length];
        if (coefficientsFile.isEmpty()) {
            System.out.println("No coefficients file provided");
            coefficients = new double[1 + expectedHeaders.length];
        } else {
            File coeffFile = new File(coefficientsFile);
            try (BufferedReader br = new BufferedReader(new FileReader(coeffFile))) {
                String[] headers = br.readLine().split("\\t");
                String[] withoutBias = new String[headers.length - 1];
                System.arraycopy(headers, 1, withoutBias, 0, withoutBias.length);

                System.arraycopy(features.names(), 0, expectedHeaders, 0, features.names().length);
                System.arraycopy(actionFeatures.names(), 0, expectedHeaders, features.names().length, actionFeatures.names().length);
                if (!Arrays.equals(withoutBias, expectedHeaders)) {
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
