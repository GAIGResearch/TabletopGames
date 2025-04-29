package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import org.json.simple.JSONObject;
import players.heuristics.AutomatedFeatures;
import players.heuristics.GLMHeuristic;
import scala.Int;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static players.heuristics.AutomatedFeatures.featureType.INTERACTION;
import static players.heuristics.AutomatedFeatures.featureType.RANGE;

public class LearnFromData {

    static int BUCKET_INCREMENT = 2;
    static int BIC_MULTIPLIER = 3;

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
        String[] dataFiles = new String[]{data};
        if (dataFile.isDirectory()) {
            convertedDataFile = data + File.separator + "ASF.txt";
            dataFiles = dataFile.list();
        }

        AutomatedFeatures asf = new AutomatedFeatures(stateFeatures, actionFeatures);
        // construct the output file by adding _ASF before the suffix (which can be anything)
        List<List<Object>> convertedData = asf.processData(convertedDataFile, dataFiles);

        // this will have created the raw data from which we now learn
        String outputFileName = Utils.getArg(args, "output", "LearnedHeuristic.json");

        learner.setStateFeatureVector(asf);

        Object learnedThing = learner.learnFrom(convertedDataFile);

        // we are now in a position to modify the features in a loop
        learnedThing = improveModel(learnedThing, (ApacheLearner) learner, convertedData.size(), dataFiles);

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

    private static Object improveModel(Object startingHeuristic,
                                       ApacheLearner learner,
                                       int n,
                                       String... dataFiles) {

        if (startingHeuristic instanceof GLMHeuristic glm) {
            AutomatedFeatures asf = (AutomatedFeatures) learner.getStateFeatureVector();
            String bestFeatureDescription = "";
            double baseBIC = bicFromAic(glm.getModel().summary().aic(), asf.names().length, n);
            double bestBIC = baseBIC;
            System.out.println("Starting modified BIC: " + baseBIC);
            List<String> excludedFeatures = new ArrayList<>();

            int iteration = 0;
            String[] rawData = dataFiles;
            do {
                AutomatedFeatures bestFeatures = null;
                baseBIC = bestBIC;  // reset baseline
                for (int i = 0; i < asf.names().length; i++) {
                    String firstFeature = asf.names()[i];
                    AutomatedFeatures.featureType type1 = asf.getFeatureType(i);

                    // TODO: RANGE features are currently not considered for interactions
                    // TODO: If interactions are on RANGE features, then do we need to freeze this bucket level?
                    // TODO: Or, somehow map the new buckets to the old buckets? [messy]
                    if (type1 == RANGE || type1 == INTERACTION)
                        continue;

                    if (type1 == AutomatedFeatures.featureType.RAW) {
                        if (excludedFeatures.contains(firstFeature))
                            continue;
                        // once a feature is below the base AIC, we save time by not checking it again
                        AutomatedFeatures adjustedASF = asf.copy();
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        adjustedASF.setBuckets(underlyingIndex, asf.getBuckets(underlyingIndex) + BUCKET_INCREMENT);
                        // TODO: This will re-process everything, which is not ideal. We really just want to re-process the column
                        // for which the bucketing is being changed. We could do this with another copy of asf that then has those
                        // features remove removed.

                        adjustedASF.processData("ImproveModel_tmp.txt", rawData);
                        learner.setStateFeatureVector(adjustedASF);

                        GLMHeuristic newHeuristic = (GLMHeuristic) learner.learnFrom("ImproveModel_tmp.txt");
                        // then find BIC
                        double newBIC = bicFromAic(newHeuristic.getModel().summary().aic(), adjustedASF.names().length, n);
                        System.out.printf("Feature: %20s, Buckets: %d, BIC: %.2f%n",
                                firstFeature, adjustedASF.getBuckets(underlyingIndex), newBIC);
                        if (newBIC < bestBIC) {
                            bestBIC = newBIC;
                            bestFeatures = adjustedASF;
                            startingHeuristic = newHeuristic;
                            bestFeatureDescription = firstFeature + " (Buckets: " + adjustedASF.getBuckets(underlyingIndex) + ")";
                        } else if (newBIC > baseBIC) {
                            excludedFeatures.add(firstFeature);
                            System.out.println("Feature " + firstFeature + " excluded");
                        }
                    }

                    for (int j = i+1; j < asf.names().length; j++) {
                        String secondFeature = asf.names()[j];
                        AutomatedFeatures.featureType type2 = asf.getFeatureType(j);

                        if (type2 == RANGE)
                            continue;
                        // currently we only support 2-way interactions between two original features
                        if (type2 == INTERACTION)
                            continue;

                        // check that this is not already an interaction
                        Pair<Integer, Integer> interaction = Pair.of(i, j);
                        if (asf.getColumnDetails().stream().anyMatch(r -> r.type() == INTERACTION &&
                                r.interaction().equals(interaction))) {
                          //  System.out.println("Already an interaction: " + firstFeature + " : " + secondFeature);
                            continue;
                        }

                        // Consider the interaction of features
                        AutomatedFeatures adjustedASF = asf.copy();
                        adjustedASF.addInteraction(i, j);
                        // providing the previous ASF means we will just calculate the new interaction
                        adjustedASF.processData("ImproveModel_tmp.txt", dataFiles);
                        learner.setStateFeatureVector(adjustedASF);

                        // TODO: Refactor to remove code repetition
                        GLMHeuristic newHeuristic = (GLMHeuristic) learner.learnFrom("ImproveModel_tmp.txt");
                        // then find AIC
                        double newBIC = bicFromAic(newHeuristic.getModel().summary().aic(), adjustedASF.names().length, n);
                        System.out.printf("Interaction: %20s, %20s, BIC: %.2f%n",
                                firstFeature, secondFeature, newBIC);

                        if (newBIC < bestBIC) {
                            bestBIC = newBIC;
                            bestFeatures = adjustedASF;
                            startingHeuristic = newHeuristic;
                            bestFeatureDescription = firstFeature + " : " + secondFeature;
                        }
                        // TODO: We can also pass the raw data directly to the learner without going through
                        // a file on disk (which may save some time)
                    }
                }
                // We then also need to set up the data file to be used as the baseline for the next iteration
                if (bestFeatures != null) {
                    String newFileName = "ImproveModel_Iter_" + iteration + ".txt";
                    bestFeatures.processData(newFileName, rawData);
                    iteration++;
                    rawData = new String[]{newFileName};
                }

                // We then update to the single best change (provided it improved on the AIC
                asf = bestFeatures;
                if (bestFeatureDescription.isEmpty()) {
                    System.out.println("No features improved AIC");
                } else {
                    System.out.println("Best feature: " + bestFeatureDescription);
                    System.out.println("New BIC: " + bestBIC);
                }
            } while (asf != null);


            // TODO: We now run through a second loop to remove any features that are not helpful

        } else {
            throw new RuntimeException("Invalid starting Model " + startingHeuristic.getClass());
        }
        return startingHeuristic;
    }

    private static double bicFromAic(double aic, int k, int n) {
        double nll = aic / 2.0 - k;
        return 2 * nll + BIC_MULTIPLIER * k * Math.log(n);
    }
}
