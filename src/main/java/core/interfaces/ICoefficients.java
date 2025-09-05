package core.interfaces;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Pair;

import java.io.*;
import java.util.*;

import static utilities.JSONUtils.loadJSONFile;

public interface ICoefficients {

    @SuppressWarnings("unchecked")
    static void removeUnusedFeatures(JSONObject coefficientsAsJSON, JSONObject featuresJson) {
        if (featuresJson.get("features") != null && featuresJson.get("features") instanceof JSONArray jsonArray) {
            // we now remove any features for which there is no matching coefficient
            JSONArray newFeaturesObject = new JSONArray();
            for (JSONObject f : (Iterable<JSONObject>) jsonArray) {
                String name = (String) f.get("name");
                if (coefficientsAsJSON.containsKey(name))
                    newFeaturesObject.add(f);
                else if (!name.contains(":")) {
                    // then check if there is an interaction that uses this feature (in which case we also want to keep it)
                    if (coefficientsAsJSON.keySet().stream()
                            .anyMatch(key -> key.toString().contains(name + ":") || key.toString().contains(":" + name))) {
                        newFeaturesObject.add(f);
                    }
                }
            }
            featuresJson.put("features", newFeaturesObject);
        }
    }

    String[] names();

    double[] coefficients();

    int[][] interactions();

    double[] interactionCoefficients();

    default int indexOf(String name) {
        return Arrays.asList(names()).indexOf(name);
    }

    default double applyCoefficients(double[] phi) {
        double retValue = coefficients()[0]; // the bias term
        for (int i = 0; i < phi.length; i++) {
            retValue += phi[i] * coefficients()[i + 1];
        }
        if (interactionCoefficients() != null)
            retValue += calculateInteractionEffects(phi);
        return retValue;
    }

    default double calculateInteractionEffects(double[] phi) {
        double retValue = 0;
        int[][] interactions = interactions();
        double[] interactionCoefficients = interactionCoefficients();
        for (int i = 0; i < interactions.length; i++) {
            double interactionValue = 1;
            for (int j : interactions[i]) {
                interactionValue *= phi[j];
            }
            retValue += interactionValue * interactionCoefficients[i];
        }
        return retValue;
    }

    default Pair<double[], List<Pair<int[], Double>>> loadModel(String coefficientsFile) {
        if (coefficientsFile.isEmpty()) {
            // in this case will default to the defaultHeuristic
            return Pair.of(new double[0], new ArrayList<>());
        } else {
            File coeffFile = new File(coefficientsFile);
            if (coeffFile.exists() && coeffFile.isFile()) {
                if (coeffFile.getName().endsWith(".csv") || coeffFile.getName().endsWith(".txt"))
                    return Pair.of(readFlatCoefficientsFile(coeffFile), new ArrayList<>());
                else if (coeffFile.getName().endsWith(".json"))
                    return readJSONCoefficientsFile(coeffFile);
                else
                    throw new AssertionError("Unknown file type : " + coeffFile);
            } else {
                throw new AssertionError("File not found : " + coeffFile);
            }
        }
    }

    default double[] readFlatCoefficientsFile(File coeffFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(coeffFile))) {
            String[] headers = br.readLine().split("\\t");
            String[] withoutBias = new String[headers.length - 1];
            System.arraycopy(headers, 1, withoutBias, 0, withoutBias.length);
            if (!Arrays.equals(withoutBias, names())) {
                throw new AssertionError("Incompatible data in file " + coeffFile);
            }
            return Arrays.stream(br.readLine().split("\\t")).mapToDouble(Double::parseDouble).toArray();
        } catch (FileNotFoundException e) {
            throw new AssertionError("File not found : " + coeffFile);
        } catch (IOException e) {
            throw new AssertionError("Error accessing : " + coeffFile);
        }
    }

    default Pair<double[], List<Pair<int[], Double>>> coefficientsFromJSON(JSONObject json) {
        // now extract all properties in the JSON file
        // and add them to the coefficients if there is no colon in the name
        // If there is a colon in the name, then split by this and look up the relevant feature
        // columns, and put an entry into JSONCoefficients
        List<Pair<int[], Double>> interactionCoefficients = new ArrayList<>();
        double[] coefficients = new double[names().length + 1]; // +1 for the bias term
        for (Object key : json.keySet()) {
            String keyStr = (String) key;
            if (keyStr.contains(":")) {
                String[] featureNames = keyStr.split(":");
                int[] featureIndices = new int[featureNames.length];
                for (int i = 0; i < featureNames.length; i++) {
                    int featureIndex = indexOf(featureNames[i]);
                    if (featureIndex == -1) {
                        throw new AssertionError("Feature not found : " + featureNames[i] + " in " + keyStr);
                    }
                    featureIndices[i] = featureIndex;
                }
                interactionCoefficients.add(Pair.of(featureIndices, getJSONAsDouble(json, key)));
            } else {
                if (keyStr.equals("BIAS")) {
                    coefficients[0] = getJSONAsDouble(json, key);
                } else {
                    int featureIndex = indexOf(keyStr);
                    if (featureIndex == -1) {
                        throw new AssertionError("Feature not found : " + keyStr);
                    }
                    coefficients[featureIndex + 1] = getJSONAsDouble(json, key);
                }
            }
        }
        return new Pair<>(coefficients, interactionCoefficients);
    }


    default Pair<double[], List<Pair<int[], Double>>> readJSONCoefficientsFile(File coeffFile) {
        // the JSON file is a list of properties, each of which is the name of a feature or set of features
        // (in which case they are separated by colons), and then the coefficient for that feature
        JSONObject json = loadJSONFile(coeffFile.toString());
        return coefficientsFromJSON(json);
    }

    default JSONObject coefficientsAsJSON() {
        // this is the reverse of readJSONCoefficientsFile
        JSONObject json = new JSONObject();
        json.put("BIAS", coefficients()[0]);
        for (int i = 0; i < coefficients().length - 1; i++) {
            if (Math.abs(coefficients()[i + 1]) > 0.00001) {
                json.put(names()[i], coefficients()[i + 1]);
            }
        }
        if (interactions() != null) {
            for (int i = 0; i < interactions().length; i++) {
                if (Math.abs(interactionCoefficients()[i]) > 0.00001) {
                    StringBuilder key = new StringBuilder();
                    for (int j : interactions()[i]) {
                        if (!key.isEmpty())
                            key.append(":");
                        key.append(names()[j]);
                    }
                    json.put(key.toString(), interactionCoefficients()[i]);
                }
            }
        }
        return json;
    }

    default String coefficientsInReadableFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"BIAS\": " + String.format("%.3g", coefficients()[0]) + ",\n");
        for (int i = 1; i < coefficients().length; i++) {
            if (Math.abs(coefficients()[i]) < 0.000001) continue; // skip zero coefficients
            sb.append("\t\"" + names()[i - 1] + "\": " + String.format("%.3g", coefficients()[i]));
            if (i < coefficients().length - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        // then for interactions
        if (interactions() != null) {
            for (int i = 0; i < interactions().length; i++) {
                if (Math.abs(interactionCoefficients()[i]) < 0.000001) continue; // skip zero coefficients
                StringBuilder key = new StringBuilder();
                for (int j = 0; j < interactions()[i].length; j++) {
                    if (j > 0)
                        key.append(":");
                    key.append(names()[interactions()[i][j]]);
                }
                sb.append("\t\"" + key + "\": " + String.format("%.3g", interactionCoefficients()[i]));
                if (i < interactions().length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private double getJSONAsDouble(JSONObject json, Object key) {
        Object value = json.get(key);
        if (value instanceof Double)
            return (Double) value;
        else if (value instanceof Long)
            return ((Long) value).doubleValue();
        else
            throw new AssertionError("Unexpected value for " + key + " : " + value);
    }

}
