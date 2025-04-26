package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import games.dominion.metrics.DomActionFeatures;
import games.dominion.metrics.DomStateFeaturesReduced;
import org.apache.spark.ml.regression.GeneralizedLinearRegressionModel;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.ml.util.HasTrainingSummary;
import org.json.simple.JSONObject;
import players.heuristics.AutomatedFeatures;
import utilities.JSONUtils;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;

public class LearnFromData {

    static int BUCKET_INCREMENT = 2;

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
        asf.processData(convertedDataFile, dataFiles);

        // this will have created the raw data from which we now learn
        String outputFileName = Utils.getArg(args, "output", "LearnedHeuristic.json");

        learner.setStateFeatureVector(asf);

        Object learnedThing = learner.learnFrom(convertedDataFile);

        // we are now in a position to modify the features in a loop
        learnedThing = improveModel(learnedThing, (ApacheLearner) learner, dataFiles);

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

    private static Object improveModel(Object startingModel,
                                       ApacheLearner learner,
                                       String... dataFiles) {
        if (startingModel instanceof GeneralizedLinearRegressionModel glm) {
            double bestAIC = glm.summary().aic();
            AutomatedFeatures asf = (AutomatedFeatures) learner.getStateFeatureVector();
            AutomatedFeatures bestFeatures = asf;

            for (int i = 0; i < asf.names().length; i++) {
                String firstFeature = asf.names()[i];
                AutomatedFeatures.featureType type1 = asf.getFeatureType(firstFeature);

                // TODO: Range features are currently not considered for interactions
                // TODO: If interactions are on bucket features, then do we need to freeze this bucket level?
                // TODO: Or, somehow map the new buckets to the old buckets? [messy]
                if (type1 == AutomatedFeatures.featureType.RANGE)
                    continue;

                if (type1 == AutomatedFeatures.featureType.RAW) {
                    // TODO: Add in the consideration of different bucketing techniques
                    AutomatedFeatures adjustedASF = asf.copy();
                    adjustedASF.setBuckets(firstFeature, asf.getBuckets(firstFeature) + BUCKET_INCREMENT);
                    adjustedASF.processData("ImproveModel_tmp.txt", dataFiles);

                    GeneralizedLinearRegressionModel newModel = (GeneralizedLinearRegressionModel) learner.learnFrom("ImproveModel_tmp.txt");
                    // then find AIC
                    double newAIC = newModel.summary().aic();
                    if (newAIC > bestAIC) {
                        bestAIC = newAIC;
                        bestFeatures = adjustedASF;
                        startingModel = newModel;
                    }
                }

                for (int j = i; j < asf.names().length; j++) {
                    String secondFeature = asf.names()[j];
                    AutomatedFeatures.featureType type2 = asf.getFeatureType(secondFeature);

                    if (type2 == AutomatedFeatures.featureType.RANGE)
                        continue;

                    // TODO: Consider the interaction of features

                }
            }

            // TODO: check to see if the best change improves the AIC by enough to warrant it's inclusion.
            // TODO: If so, then re-iterate on the new features

        } else {
            throw new RuntimeException("Invalid starting Model " + startingModel.getClass());
        }
        return startingModel;
    }
}
