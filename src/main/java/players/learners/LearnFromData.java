package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import org.json.simple.JSONObject;
import evaluation.features.AutomatedFeatures;
import players.heuristics.GLMHeuristic;
import utilities.JSONUtils;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static evaluation.features.AutomatedFeatures.featureType.*;
import static players.learners.AbstractLearner.Target.*;

public class LearnFromData {

    static int BUCKET_INCREMENT = 2;
    final int baseBicMultiplier;
    int bicMultiplier;
    int bicTimer;

    AbstractLearner learner;
    String outputFileName;
    IStateFeatureVector stateFeatures;
    IActionFeatureVector actionFeatures;
    String data;


    public static void main(String[] args) {

        String stateClassString = Utils.getArg(args, "state", "");
        IStateFeatureVector stateFeatures = stateClassString.isEmpty() ? null : JSONUtils.loadClass(stateClassString);
        String actionClassString = Utils.getArg(args, "action", "");
        IActionFeatureVector actionFeatures = actionClassString.isEmpty() ? null : JSONUtils.loadClass(actionClassString);

        if (stateFeatures == null && actionFeatures == null) {
            System.out.println("Need to specify a state or action feature vector");
            System.exit(0);
        }

        // We need the learner config
        String learnerFile = Utils.getArg(args, "learner", "");
        if (learnerFile.isEmpty()) {
            System.out.println("Need to specify a learner");
            System.exit(0);
        }
        AbstractLearner learner = JSONUtils.loadClass(learnerFile);

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

        String outputFileName = Utils.getArg(args, "output", "LearnedHeuristic.json");

        LearnFromData learnFromData = new LearnFromData(data, stateFeatures, actionFeatures,
                outputFileName, learner, 3, 30);
        learnFromData.learn();
    }


    public LearnFromData(String data, IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures,
                         String outputFileName, AbstractLearner learner, int bicMultiplier, int bicTimer) {
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
        this.outputFileName = outputFileName;
        this.learner = learner;
        this.data = data;
        this.baseBicMultiplier = bicMultiplier;
        this.bicMultiplier = bicMultiplier;
        this.bicTimer = bicTimer;
    }

    public Object learn() {
        File dataFile = new File(data);
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

        if (learner.targetType == ACTION_ADV || learner.targetType == ACTION_CHOSEN ||
                learner.targetType == ACTION_SCORE || learner.targetType == ACTION_VISITS) {
            learner.setActionFeatureVector(asf);
        } else {
            learner.setStateFeatureVector(asf);
        }
        // this creates the extended AutomatedFeatures, and fits to this; before considering any interactions, bucketing or pruning
        Object learnedThing = learner.learnFrom(convertedDataFile);

        // we are now in a position to modify the features in a loop
        learnedThing = improveModel(learnedThing, learner, convertedData.size(), convertedDataFile);

        if (learnedThing instanceof IToJSON toJSON) {
            JSONObject json = toJSON.toJSON();
            JSONUtils.writeJSON(json, outputFileName);
            // this next line is to ensure that the filtering of INTERACTIVE features is taken account of in the returned heuristic
            learnedThing = JSONUtils.loadClassFromJSON(json);
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
        return learnedThing;
    }

    private Object improveModel(Object startingHeuristic,
                                AbstractLearner learner,
                                int n,
                                String... dataFiles) {

        long startTime = System.currentTimeMillis();
        if (startingHeuristic instanceof GLMHeuristic glm) {
            AutomatedFeatures asf;
            if (learner.targetType == ACTION_SCORE || learner.targetType == ACTION_VISITS ||
                    learner.targetType == ACTION_ADV || learner.targetType == ACTION_CHOSEN) {
                asf = (AutomatedFeatures) learner.getActionFeatureVector();
            } else {
                asf = (AutomatedFeatures) learner.getStateFeatureVector();
            }
            String bestFeatureDescription = "";
            double baseBIC = bicFromAic(glm.getModel().summary().aic(), asf.names().length, n);
            double bestBIC = baseBIC;
            System.out.println("Starting modified BIC: " + baseBIC);
            List<String> excludedFeatures = new ArrayList<>();
            List<String> excludedBucketFeatures = new ArrayList<>();
            List<String> excludedInteractionFeatures = new ArrayList<>();
            int iteration = 0;
            String dataDirectory = dataFiles[0].substring(0, dataFiles[0].lastIndexOf(File.separator));
            String outputFile = dataDirectory + File.separator + "ImproveModel_tmp.txt";

            String[] rawData = dataFiles;
            AutomatedFeatures bestFeatures;

            // we check for any zero coefficients in startingHeuristic
            // and exclude those features from the search
            for (int i = 0; i < asf.names().length; i++) {
                if (Math.abs(glm.coefficients()[i + 1]) < 0.00001) {
                    excludedFeatures.add(asf.names()[i]);
                }
            }
            if (!excludedFeatures.isEmpty()) {
                System.out.println("Excluding features with zero coefficients: " + excludedFeatures);
            }

            do {
                bestFeatures = null;
                baseBIC = bestBIC;  // reset baseline
                for (int i = 0; i < asf.names().length; i++) {
                    if (excludedFeatures.contains(asf.names()[i]))
                        continue;  // skip as it has a zero coefficient

                    String firstFeature = asf.names()[i];
                    AutomatedFeatures.featureType type1 = asf.getFeatureType(i);

                    if (type1 == RANGE) {
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        String underlyingFeature = asf.names()[underlyingIndex];
                        if (!excludedBucketFeatures.contains(underlyingFeature))
                            continue;  // we only consider RANGE features for interactions once the bucketing is fixed
                    }
                    if (type1 == AutomatedFeatures.featureType.RAW && !excludedBucketFeatures.contains(firstFeature)) {
                        // once a feature is below the base AIC, we save time by not checking it again
                        AutomatedFeatures adjustedASF = asf.copy();
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        adjustedASF.setBuckets(underlyingIndex, asf.getBuckets(underlyingIndex) + BUCKET_INCREMENT);

                        FeatureAnalysisResult result = processNewFeature(adjustedASF, outputFile, rawData, learner, n);

                        if (result.newBIC < bestBIC) {
                            bestBIC = result.newBIC;
                            bestFeatures = adjustedASF;
                            startingHeuristic = result.newHeuristic;
                            bestFeatureDescription = firstFeature + " (Buckets: " + adjustedASF.getBuckets(underlyingIndex) + ")";
                        } else if (result.newBIC > baseBIC) {
                            excludedBucketFeatures.add(firstFeature);
                        }
                    }

                    for (int j = i; j < asf.names().length; j++) {
                        String secondFeature = asf.names()[j];
                        String interactionName = firstFeature + ":" + secondFeature;
                        AutomatedFeatures.featureType type2 = asf.getFeatureType(j);

                        if (type2 == RANGE) {
                            int underlyingIndex = asf.getUnderlyingIndex(j);
                            String underlyingFeature = asf.names()[underlyingIndex];
                            if (!excludedBucketFeatures.contains(underlyingFeature))
                                continue;  // we only consider RANGE features for interactions once the bucketing is fixed
                        }
                        if (excludedFeatures.contains(secondFeature))
                            continue;  // skip as it has a zero coefficient


                        if (excludedInteractionFeatures.contains(interactionName))
                            continue;  // we've already checked this one and it failed to help

                        // check that this is not an interaction between two mutually exclusive ENUM/RANGE features
                        if ((asf.getFeatureType(j) == ENUM || asf.getFeatureType(j) == RANGE) &&
                                asf.getUnderlyingIndex(i) == asf.getUnderlyingIndex(j)) {
                            continue;
                        }

                        // check that this is not already an interaction
                        if (asf.getColumnDetails().stream().anyMatch(r -> r.type() == INTERACTION &&
                                r.name().equals(interactionName))) {
                            //  System.out.println("Already an interaction: " + firstFeature + " : " + secondFeature);
                            continue;
                        }

                        // Consider the interaction of features
                        AutomatedFeatures adjustedASF = asf.copy();
                        adjustedASF.addInteraction(i, j);
                        FeatureAnalysisResult result = processNewFeature(adjustedASF, outputFile, rawData, learner, n);

                        if (result.newBIC < bestBIC) {
                            bestBIC = result.newBIC;
                            bestFeatures = adjustedASF;
                            startingHeuristic = result.newHeuristic;
                            bestFeatureDescription = interactionName;
                        } else if (result.newBIC > baseBIC) {
                            excludedInteractionFeatures.add(interactionName);
                        }
                    }
                }
                // We then also need to set up the data file to be used as the baseline for the next iteration
                if (bestFeatures != null) {
                    String newFileName = dataDirectory + File.separator + "ImproveModel_Iter_" + iteration + ".txt";
                    bestFeatures.processData(newFileName, rawData);
                    iteration++;
                    rawData = new String[]{newFileName};
                }

                // We then update to the single best change (provided it improved on the AIC
                if (bestFeatures == null) {
                    System.out.println("No features improved AIC");
                } else {
                    System.out.println("Best feature: " + bestFeatureDescription);
                    System.out.println("New BIC: " + bestBIC);
                    asf = bestFeatures;
                }

                // increment bicMultiplier if time is getting on
                int minutesElapsed = (int) ((System.currentTimeMillis() - startTime) / 60000);
                bicMultiplier = (minutesElapsed / bicTimer + 1) * baseBicMultiplier;
            } while (bestFeatures != null);

            List<AutomatedFeatures.ColumnDetails> interactionColumns = asf.getColumnDetails().stream()
                    .filter(r -> r.type() == INTERACTION).toList();
            List<String> columnsWithInteraction = interactionColumns.stream()
                    .flatMap(
                            cd -> Arrays.stream(cd.name().split(":")))
                    .distinct()
                    .toList();
            if (!columnsWithInteraction.isEmpty())
                System.out.println("Columns with interactions: " + columnsWithInteraction);
            do {
                bestFeatures = null;
                baseBIC = bestBIC; // reset baseline
                for (int i = 0; i < asf.names().length; i++) {
                    String featureToRemove = asf.names()[i];
                    if (excludedFeatures.contains(featureToRemove))
                        continue;
                    if (columnsWithInteraction.contains(featureToRemove))
                        continue; // we don't want to remove a feature that is part of an interaction

                    AutomatedFeatures adjustedASF = asf.copy();
                    adjustedASF.removeFeature(i);

                    // we always use the data file from the last iteration of building the model (as this has all the data)
                    String newFileName = dataDirectory + File.separator +
                            (iteration > 0 ? "ImproveModel_Iter_" + (iteration - 1) + ".txt" :
                                    "ImproveModel_tmp.txt");
                    FeatureAnalysisResult result = processNewFeature(adjustedASF, newFileName, new String[0], learner, n);

                    if (result.newBIC < bestBIC) {
                        bestBIC = result.newBIC;
                        bestFeatures = adjustedASF;
                        startingHeuristic = result.newHeuristic;
                        bestFeatureDescription = String.format("Removed Feature: %20s, BIC: %.2f", featureToRemove, bestBIC);
                    } else if (result.newBIC > baseBIC) {
                        excludedFeatures.add(featureToRemove);
                    }
                }

                if (bestFeatures != null) {
                    asf = bestFeatures;
                    System.out.println("Best feature modification: " + bestFeatureDescription);
                    System.out.println("New BIC: " + bestBIC);
                } else {
                    System.out.println("No features improved BIC when removed");
                }
            } while (bestFeatures != null);

        } else {
            throw new RuntimeException("Invalid starting Model " + startingHeuristic.getClass());
        }
        return startingHeuristic;
    }

    private record FeatureAnalysisResult(
            AutomatedFeatures adjustedASF,
            GLMHeuristic newHeuristic,
            double newBIC) {
    }

    FeatureAnalysisResult processNewFeature(AutomatedFeatures asf,
                                                      String outputFile,
                                                      String[] rawData,
                                                      AbstractLearner learner,
                                                      int n) {

        if (rawData != null && rawData.length > 0)
            asf.processData(outputFile, rawData);

        learner.setStateFeatureVector(asf);

        GLMHeuristic newHeuristic = (GLMHeuristic) learner.learnFrom(outputFile);
        double newBIC = bicFromAic(newHeuristic.getModel().summary().aic(), asf.names().length, n);
        return new FeatureAnalysisResult(asf, newHeuristic, newBIC);
    }

    private double bicFromAic(double aic, int k, int n) {
        double nll = aic / 2.0 - k;
        return 2 * nll + bicMultiplier * k * Math.log(n);
    }
}
