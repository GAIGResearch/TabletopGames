package players.heuristics;

import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;

import static utilities.JSONUtils.loadJSONFile;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public abstract class AbstractStateHeuristic implements IStateHeuristic {

    protected IStateFeatureVector features;
    protected double[] coefficients;
    protected Map<int[], Double> interactionCoefficients = new HashMap<>();
    protected IStateHeuristic defaultHeuristic;

    public AbstractStateHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClassName) {
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
        loadModel(coefficientsFile);
    }

    public AbstractStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        loadModel(coefficientsFile);
    }

    private void loadModel(String coefficientsFile) {
        if (coefficientsFile.isEmpty()) {
            // in this case will default to the defaultHeuristic
        } else {
            File coeffFile = new File(coefficientsFile);
            if (coeffFile.exists() && coeffFile.isFile()) {
                if (coeffFile.getName().endsWith(".csv") || coeffFile.getName().endsWith(".txt"))
                    readFlatCoefficientsFile(coeffFile);
                else if (coeffFile.getName().endsWith(".json"))
                    readJSONCoefficientsFile(coeffFile);
                else
                    throw new AssertionError("Unknown file type : " + coeffFile);
            } else {
                throw new AssertionError("File not found : " + coeffFile);
            }
        }
    }

    private void readFlatCoefficientsFile(File coeffFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(coeffFile))) {
            String[] headers = br.readLine().split("\\t");
            String[] withoutBias = new String[headers.length - 1];
            System.arraycopy(headers, 1, withoutBias, 0, withoutBias.length);
            if (!Arrays.equals(withoutBias, features.names())) {
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


    private void readJSONCoefficientsFile(File coeffFile) {
        // the JSON file is a list of properties, each of which is the name of a feature or set of features
        // (in which case they are separated by colons), and then the coefficient for that feature
        JSONObject json = loadJSONFile(coeffFile.toString());
        // now extract all properties in the JSON file
        // and add them to the coefficients if there is no colon in the name
        // If there is a colon in the name, then split by this and look up the relevant feature
        // columns, and put an entry into JSONCoefficients
        for (Object key : json.keySet()) {
            String keyStr = (String) key;
            if (keyStr.contains(":")) {
                String[] featureNames = keyStr.split(":");
                int[] featureIndices = new int[featureNames.length];
                for (int i = 0; i < featureNames.length; i++) {
                    int featureIndex = features.indexOf(featureNames[i]);
                    if (featureIndex == -1) {
                        throw new AssertionError("Feature not found : " + featureNames[i] + " in " + keyStr);
                    }
                    featureIndices[i] = featureIndex;
                }
                interactionCoefficients.put(featureIndices, (Double) json.get(key));
            } else {
                int featureIndex = features.indexOf(keyStr);
                if (featureIndex == -1) {
                    throw new AssertionError("Feature not found : " + keyStr);
                }
                coefficients[featureIndex] = (Double) json.get(key);
            }
        }
    }

    protected double applyCoefficients(double[] phi) {
        double retValue = coefficients[0]; // the bias term
        for (int i = 0; i < phi.length; i++) {
            retValue += phi[i] * coefficients[i + 1];
        }
        if (!interactionCoefficients.isEmpty())
            retValue += calculateInteractionEffects(phi);
        return retValue;
    }
    private double calculateInteractionEffects(double[] phi) {
        double retValue = 0;
        for (int[] interaction : interactionCoefficients.keySet()) {
            double interactionValue = 1;
            for (int i : interaction) {
                interactionValue *= phi[i];
            }
            retValue += interactionValue * interactionCoefficients.get(interaction);
        }
        return retValue;
    }
}
