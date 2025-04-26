package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import games.dominion.metrics.DomActionFeatures;
import games.dominion.metrics.DomStateFeaturesReduced;
import org.json.simple.JSONObject;
import players.heuristics.AutomatedFeatures;
import utilities.JSONUtils;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;

public class LearnFromData {


    public static void main(String[] args) {

        String stateClassString = Utils.getArg(args, "state", "");
        IStateFeatureVector stateFeatures = stateClassString.isEmpty() ? null : JSONUtils.loadClassFromString(stateClassString);
        String actionClassString = Utils.getArg(args, "action", "");
        IActionFeatureVector actionFeatures = actionClassString.isEmpty() ? null : JSONUtils.loadClassFromString(actionClassString);

        if (stateFeatures == null && actionFeatures == null) {
            System.out.println("Need to specify a state or action feature vector");
            System.exit(0);
        }

        // We need the learner config
        String learnerFile = Utils.getArg(args, "learner", "");
        if (learnerFile.isEmpty()) {
            System.out.println("Need to specify a learner file");
            System.exit(0);
        }
        AbstractLearner learner = JSONUtils.loadClassFromFile(learnerFile);

        String data = Utils.getArg(args, "data", "");
        if (data.isEmpty()) {
            System.out.println("Need to specify a data file");
            System.exit(0);
        }
        File dataFile = new File(data);
        if (!dataFile.exists()) {
            System.out.println("Data file " + data + " does not exist");
            System.exit(0);
        }
        String convertedDataFile = data.replaceAll("\\.[^.]+$", "_ASF$0");
        String[] dataFiles = new String[] {data};
        if (dataFile.isDirectory()) {
            convertedDataFile = data + File.separator + "ASF.txt";
            dataFiles = dataFile.list();
        }

        AutomatedFeatures asf = new AutomatedFeatures(stateFeatures, actionFeatures);
        // construct the output file by adding _ASF before the suffix (which can be anything)
        asf.processData(convertedDataFile, dataFiles);

        // this will have created the raw data from which we now learn
        String outputFileName = Utils.getArg(args, "output", "LearnedHeuristic.json");

        learner.setStateFeatureVector(asf);

        Object learnedThing = learner.learnFrom(convertedDataFile);
        if (learnedThing instanceof IToJSON toJSON) {
            JSONObject json = toJSON.toJSON();
            JSONUtils.writeJSON(json, outputFileName);
        }
        // write the coefficients to a file
        if (learnedThing instanceof ICoefficients coefficients) {
            String coefficientsFile = outputFileName.replace(".json", "_coeffsOnly.json");
            String output = coefficients.coefficientsInReadableFormat();
            // write output to coefficientsFile

            try (FileWriter writer = new FileWriter(coefficientsFile)) {
                writer.write(output);
            } catch (Exception e) {
                System.out.println("Error writing file " + coefficientsFile + " : " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
