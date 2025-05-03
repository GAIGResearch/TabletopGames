package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import org.json.simple.JSONObject;
import players.heuristics.AutomatedFeatures;
import players.heuristics.GLMHeuristic;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static players.heuristics.AutomatedFeatures.featureType.*;
import static players.learners.AbstractLearner.Target.*;

public class LearnFromData {

    static int BUCKET_INCREMENT = 2;
    static int BIC_MULTIPLIER = 3;

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
                outputFileName, learner);
        learnFromData.learn();
    }


    public LearnFromData(String data, IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures,
                         String outputFileName, AbstractLearner learner) {
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
        this.outputFileName = outputFileName;
        this.learner = learner;
        this.data = data;
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

        // TODO: Ideally we want to split ASF into distinct state and action features
        // to reduce inefficiencies in recalculating all the state features for every action
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

    private static Object improveModel(Object startingHeuristic,
                                       AbstractLearner learner,
                                       int n,
                                       String... dataFiles) {

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

                    // TODO: RANGE features are currently not considered for interactions
                    // TODO: If interactions are on RANGE features, then do we need to freeze this bucket level?
                    // TODO: Or, somehow map the new buckets to the old buckets? [messy]

                    // TODO: INTERACTION features are not considered for interactions either
                    // TODO: Removing the limit on simple 2-way interactions is a good later enhancement
                    // TODO: as any arbitrary level interaction can be built up from a progressive series of 2-way interactions
                    if (type1 == RANGE || type1 == INTERACTION)
                        continue;

                    if (type1 == AutomatedFeatures.featureType.RAW && !excludedBucketFeatures.contains(firstFeature)) {
                        // once a feature is below the base AIC, we save time by not checking it again
                        AutomatedFeatures adjustedASF = asf.copy();
                        int underlyingIndex = asf.getUnderlyingIndex(i);
                        adjustedASF.setBuckets(underlyingIndex, asf.getBuckets(underlyingIndex) + BUCKET_INCREMENT);

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
                            excludedBucketFeatures.add(firstFeature);
                            //            System.out.println("Feature " + firstFeature + " excluded");
                        }
                    }

                    for (int j = i + 1; j < asf.names().length; j++) {
                        // If the columns have the same underlying column, then skip
                        if (asf.getUnderlyingIndex(i) != -1 && asf.getUnderlyingIndex(i) == asf.getUnderlyingIndex(j))
                            continue;

                        String secondFeature = asf.names()[j];
                        AutomatedFeatures.featureType type2 = asf.getFeatureType(j);

                        if (type2 == RANGE)
                            continue;
                        // currently we only support 2-way interactions between two original features
                        if (type2 == INTERACTION)
                            continue;
                        if (excludedFeatures.contains(secondFeature))
                            continue;  // skip as it has a zero coefficient


                        if (excludedInteractionFeatures.contains(firstFeature + ":" + secondFeature))
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
                        } else if (newBIC > baseBIC) {
                            excludedInteractionFeatures.add(firstFeature + ":" + secondFeature);
                            //                  System.out.println("Interaction " + firstFeature + ":" + secondFeature + " excluded");
                        }
                        // TODO: (Low priority) We can also pass the raw data directly to the learner without going through
                        // a file on disk (which may save some time). Although only with OLS will this really be noticeable
                        // given the performance bottleneck is the model fitting
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
                if (bestFeatures == null) {
                    System.out.println("No features improved AIC");
                } else {
                    System.out.println("Best feature: " + bestFeatureDescription);
                    System.out.println("New BIC: " + bestBIC);
                    asf = bestFeatures;
                }
            } while (bestFeatures != null);

            List<String> excludedReductionFeatures = new ArrayList<>();
            List<AutomatedFeatures.ColumnDetails> interactionColumns = asf.getColumnDetails().stream()
                    .filter(r -> r.type() == INTERACTION).toList();
            List<String> columnsWithInteraction = interactionColumns.stream()
                    .flatMap(
                            cd -> Arrays.stream(cd.name().split(":")))
                    .distinct()
                    .toList();
            //      System.out.println("Columns with interactions: " + columnsWithInteraction);
            do {
                bestFeatures = null;
                baseBIC = bestBIC; // reset baseline
                for (int i = 0; i < asf.names().length; i++) {
                    String featureToRemove = asf.names()[i];
                    if (excludedReductionFeatures.contains(featureToRemove))
                        continue;
                    if (asf.getFeatureType(i) != RAW || asf.getBuckets(asf.getUnderlyingIndex(i)) > 1)
                        continue;  // for the moment, we only consider RAW features with no buckets
                    if (columnsWithInteraction.contains(featureToRemove))
                        continue; // we don't want to remove a feature that is part of an interaction

                    AutomatedFeatures adjustedASF = asf.copy();
                    adjustedASF.removeFeature(i);
                    learner.setStateFeatureVector(adjustedASF);

                    // we always use the data file from the last iteration of building the model (as this has all the data)
                    GLMHeuristic newHeuristic = (GLMHeuristic) learner.learnFrom("ImproveModel_Iter_" + (iteration - 1) + ".txt");
                    double newBIC = bicFromAic(newHeuristic.getModel().summary().aic(), adjustedASF.names().length, n);
                    //              System.out.printf("Considering Feature: %20s, BIC: %.2f (%d/%d)%n", featureToRemove, newBIC, adjustedASF.names().length, asf.names().length);

                    if (newBIC < bestBIC) {
                        bestBIC = newBIC;
                        bestFeatures = adjustedASF;
                        startingHeuristic = newHeuristic;
                        bestFeatureDescription = String.format("Removed Feature: %20s, BIC: %.2f", featureToRemove, newBIC);
                    } else if (newBIC > baseBIC) {
                        excludedReductionFeatures.add(featureToRemove);
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

    private static double bicFromAic(double aic, int k, int n) {
        double nll = aic / 2.0 - k;
        return 2 * nll + BIC_MULTIPLIER * k * Math.log(n);
    }
}
