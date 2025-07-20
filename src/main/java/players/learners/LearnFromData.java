package players.learners;

import core.interfaces.*;
import org.json.simple.JSONObject;
import evaluation.features.AutomatedFeatures;
import players.heuristics.GLMHeuristic;
import utilities.JSONUtils;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.features.AutomatedFeatures.featureType.*;
import static java.util.stream.Collectors.joining;
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
    boolean debug = false;
    int maxRecords = 10000;


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
        long startTime = System.currentTimeMillis();
        File dataFile = new File(data);
        String convertedDataFile = data.replaceAll("\\.[^.]+$", "_ASF$0");
        String[] dataFiles = new String[]{data};
        if (dataFile.isDirectory()) {
            convertedDataFile = data + File.separator + "ASF.txt";
            dataFiles = dataFile.list();
        }

        AutomatedFeatures asf = new AutomatedFeatures(stateFeatures, actionFeatures);
        // construct the output file by adding _ASF before the suffix (which can be anything)
        List<List<Object>> convertedData = asf.processData(true, convertedDataFile, maxRecords, dataFiles);

        // this will have created the raw data from which we now learn
        // whichever of state/action features is not null will prompt the type of Heuristic learned
        if (actionFeatures != null)
            learner.setActionFeatureVector(asf);
        else
            learner.setStateFeatureVector(asf);
        // this creates the extended AutomatedFeatures, and fits to this; before considering any interactions, bucketing or pruning
        int startingFeatureCount = learner.featureCount();
        Object learnedThing = learner.learnFrom(convertedDataFile);

        // we are now in a position to modify the features in a loop
        learnedThing = improveModel(learnedThing, learner, convertedData.size(), convertedDataFile);

        if (learnedThing instanceof IToJSON toJSON) {
            JSONObject json = toJSON.toJSON();
            JSONUtils.writeJSON(json, outputFileName);
            // this next line is to ensure that the filtering of INTERACTIVE features is taken account of in the returned heuristic
            // toJSON has also filtered the ASF into two components, one for the state values and one for action values
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
        long endTime = System.currentTimeMillis();
        System.out.printf("Learned heuristic in %d minutes with %d -> %d features and %d rows%n",
                (endTime - startTime) / 60000, startingFeatureCount, learner.featureCount(), convertedData.size());
        return learnedThing;
    }

    private Object improveModel(Object startingHeuristic,
                                AbstractLearner learner,
                                int n,
                                String... dataFiles) {

        long startTime = System.currentTimeMillis();
        if (startingHeuristic instanceof GLMHeuristic glm) {
            AutomatedFeatures asf = (AutomatedFeatures) (learner.getActionFeatureVector() != null ? learner.getActionFeatureVector() : learner.getStateFeatureVector());
            String bestFeatureDescription = "";
            double baseBIC = bicFromAic(glm.getModel().summary().aic(), asf.names().length, n);
            double bestBIC = baseBIC;
            System.out.println("Starting modified BIC: " + baseBIC);
            List<String> excludedFeatures = new ArrayList<>();
            List<String> excludedBucketFeatures = new ArrayList<>();
            List<String> excludedInteractionFeatures = new ArrayList<>();
            List<String> featuresToKeep = new ArrayList<>();
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
                asf = removeExcludedFeatures(excludedFeatures, asf);
            }

            do {
                bestFeatures = null;
                if (debug)
                    System.out.printf("Iteration %d, current feature count %d / %d%n", iteration, asf.names().length, learner.featureCount());
                baseBIC = bestBIC;  // reset baseline
                for (int i = 0; i < asf.names().length; i++) {

                    String firstFeature = asf.names()[i];
                    AutomatedFeatures.featureType type1 = asf.getFeatureType(i);

                    if (type1 == RANGE) {
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        String underlyingFeature = asf.names()[underlyingIndex];
                        if (!excludedBucketFeatures.contains(underlyingFeature))
                            continue;  // we only consider RANGE features for interactions once the bucketing is fixed
                    }
                    if (type1 == AutomatedFeatures.featureType.RAW && !excludedBucketFeatures.contains(firstFeature)) {
                        // once a feature is below the base AIC, we save time by not checking it for bucketing again
                        AutomatedFeatures adjustedASF = asf.copy();
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        adjustedASF.setBuckets(underlyingIndex, asf.getBuckets(underlyingIndex) + BUCKET_INCREMENT);

                        FeatureAnalysisResult result = processNewFeature(adjustedASF, outputFile, rawData, learner, n);

                        if (result.newBIC < bestBIC) {
                            bestBIC = result.newBIC;
                            bestFeatures = result.adjustedASF;
                            startingHeuristic = result.newHeuristic;
                            bestFeatureDescription = firstFeature + " (Buckets: " + result.adjustedASF.getBuckets(underlyingIndex) + ")";
                        } else if (result.newBIC > baseBIC) {
                            if (debug)
                                System.out.println("Excluding feature " + firstFeature + " with buckets " +
                                        adjustedASF.getBuckets(underlyingIndex) + " as it did not improve BIC");
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

                        if (debug)
                            System.out.printf("\tConsidered interaction: %s, new BIC: %.2f%n", interactionName, result.newBIC);
//                        System.out.printf("\tCoefficients: %s%n",
//                                result.newHeuristic.coefficients() != null ?
//                                        Arrays.stream(result.newHeuristic.coefficients()).mapToObj(d -> String.format("%.2f", d)).collect(joining("|")) : "[]");

                        if (result.newBIC < bestBIC) {
                            bestBIC = result.newBIC;
                            bestFeatures = result.adjustedASF;
                            startingHeuristic = result.newHeuristic;
                            bestFeatureDescription = interactionName;
                        } else if (result.newBIC > baseBIC) {
                            // if an interaction worsens the BIC, then we exclude it from future consideration
                            // on the basis that this is *unlikely* to improve in future iterations [although it might]
                            excludedInteractionFeatures.add(interactionName);
                        }
                    }

                    // Then consider removing this feature (if it is not part of an interaction, and we have finished bucketing)
                    String featureToRemove = asf.names()[i];
                    // we *can* remove interactions that are in other interactions (just not RANGE or RAW features)
                    if (asf.getFeatureType(i) != INTERACTION && (featuresToKeep.contains(featureToRemove) || usedInInteraction(asf, featureToRemove)))
                        continue; // we don't want to remove a RAW/RANGE feature that is part of an interaction, or that previous removal damaged BIC

                    AutomatedFeatures adjustedASF = asf.copy();
                    adjustedASF.removeFeature(i);

                    // we always use the data file from this iteration of building the model (as this has all the data)
                    FeatureAnalysisResult result = processNewFeature(adjustedASF, outputFile, new String[0], learner, n);

                    if (debug)
                        System.out.printf("\tConsidered feature removal: %s, new BIC: %.2f%n", featureToRemove, result.newBIC);
//                    System.out.printf("\tCoefficients: %s%n",
//                            result.newHeuristic.coefficients() != null ?
//                                    Arrays.stream(result.newHeuristic.coefficients()).mapToObj(d -> String.format("%.2f", d)).collect(joining("|")) : "[]");
                    if (result.newBIC < bestBIC) {
                        bestBIC = result.newBIC;
                        bestFeatures = result.adjustedASF;
                        startingHeuristic = result.newHeuristic;
                        bestFeatureDescription = String.format("Removed Feature %s", featureToRemove);
                    } else if (result.newBIC > baseBIC + bicMultiplier * asf.names().length) {
                        featuresToKeep.add(featureToRemove);
                    }
                }

                if (bestFeatureDescription.startsWith("Removed Feature")) {
                    // If the best is a removal, we need to add to excludedFeatures so that we exclude it from future iterations
                    // otherwise processNewData will keep adding it back in
                    excludedFeatures.add(bestFeatureDescription.substring(bestFeatureDescription.lastIndexOf("is ") + 3));
                }
                // We then also need to set up the data file to be used as the baseline for the next iteration
                if (bestFeatures != null) {
                    String newFileName = dataDirectory + File.separator + "ImproveModel_Iter_" + iteration + ".txt";
                    bestFeatures.processData(false, newFileName, maxRecords, rawData);
                    // then remove excluded features from the bestFeatures (these are always in the file so it always contains the original raw data)
                    bestFeatures = removeExcludedFeatures(excludedFeatures, bestFeatures);
                    iteration++;
                    rawData = new String[]{newFileName};
                }

                // We then update to the single best change (provided it improved on the BIC)
                if (bestFeatures == null) {
                    System.out.println("No feature changes improved BIC");
                } else {
                    System.out.printf("Best feature with BIC: %.2f is %s%n", bestBIC, bestFeatureDescription);
                    if (debug) {
                        System.out.printf("\tCoefficients: %s%n",
                                glm.coefficients() != null ?
                                        Arrays.stream(glm.coefficients()).mapToObj(d -> String.format("%.2f", d)).collect(joining("|")) : "[]");
                    }
                    asf = bestFeatures;
                }

                // increment bicMultiplier if time is getting on
                int minutesElapsed = (int) ((System.currentTimeMillis() - startTime) / 60000);
                if (minutesElapsed > bicTimer) {
                    System.out.printf("Time elapsed: %d minutes, increasing BIC multiplier from %d to %d%n",
                            minutesElapsed, bicMultiplier, bicMultiplier + baseBicMultiplier);
                    startTime = System.currentTimeMillis();
                    bicMultiplier = bicMultiplier + baseBicMultiplier;
                    // then adjust current bestBIC to reflect the new multiplier
                    bestBIC = bicFromAic(glm.getModel().summary().aic(), asf.names().length, n);
                }
            } while (bestFeatures != null);

        } else {
            throw new RuntimeException("Invalid starting Model " + startingHeuristic.getClass());
        }
        return startingHeuristic;
    }

    private AutomatedFeatures removeExcludedFeatures(List<String> excludedFeatures, AutomatedFeatures asf) {
        AutomatedFeatures retValue = asf.copy();
        List<Integer> indicesToRemove = IntStream.range(0, asf.names().length)
                .filter(i -> excludedFeatures.contains(asf.names()[i]))
                .boxed().sorted().toList();
        // remove in reverse order to avoid index shifting
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            int indexToRemove = indicesToRemove.get(i);
            //         System.out.println("Removing feature: " + finalAsf.names()[indexToRemove]);
            retValue.removeFeature(indexToRemove);
        }
        return retValue;
    }

    private boolean usedInInteraction(AutomatedFeatures asf, String feature) {
        List<AutomatedFeatures.ColumnDetails> interactionColumns = asf.getColumnDetails().stream()
                .filter(r -> r.type() == INTERACTION).toList();
        List<String> columnsWithInteraction = interactionColumns.stream()
                .flatMap(
                        cd -> Arrays.stream(cd.name().split(":")))
                .distinct()
                .toList();
        return columnsWithInteraction.contains(feature);
    }

    public void setMaxRecords(int i) {
        maxRecords = i;
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

        AutomatedFeatures localASF = asf.copy();
        if (rawData != null && rawData.length > 0)
            localASF.processData(false, outputFile, maxRecords, rawData);

        if (learner.actionFeatureVector != null)
            learner.setActionFeatureVector(localASF);
        else
            learner.setStateFeatureVector(localASF);

        GLMHeuristic newHeuristic = (GLMHeuristic) learner.learnFrom(outputFile);
        double newBIC = bicFromAic(newHeuristic.getModel().summary().aic(), localASF.names().length, n);
        return new FeatureAnalysisResult(localASF, newHeuristic, newBIC);
    }

    private double bicFromAic(double aic, int k, int n) {
        double nll = aic / 2.0 - k;
        return 2 * nll + bicMultiplier * k * Math.log(n);
    }
}
