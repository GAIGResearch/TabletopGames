package players.learners;

import core.interfaces.ICoefficients;
import core.interfaces.IToJSON;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;

public class LearnFromData {

    public static void main(String[] args) {
        // We need the learner config, and the data files as input (either a single file, or a directory), and the output file
        String learnerFile = Utils.getArg(args, "learner", "");
        if (learnerFile.isEmpty()) {
            System.out.println("Need to specify a learner file");
            System.exit(0);
        }
        AbstractLearner learner = JSONUtils.loadClassFromFile(learnerFile);

        String outputFileName = Utils.getArg(args, "output", "");
        if (outputFileName.isEmpty()) {
            System.out.println("Need to specify an output file");
            System.exit(0);
        }
        String inputFileName = Utils.getArg(args, "input", "");
        if (inputFileName.isEmpty()) {
            System.out.println("Need to specify an input file");
            System.exit(0);
        }
        String[] files;
        File inputFile = new File(inputFileName);
        if (inputFile.isDirectory()) {
            // load all Data
            files = inputFile.list();
        } else {
            files = new String[]{inputFileName};
        }

        Object learnedThing = learner.learnFrom(files);
        if (learnedThing instanceof IToJSON toJSON) {
            JSONObject json = toJSON.toJSON();
            JSONUtils.writeJSON(json, outputFileName);
        }
        // write the coefficients to a file
        if (learnedThing instanceof ICoefficients coefficients) {
            String coefficientsFile = outputFileName.replace(".json", "_coeffsOnly.json");
            String output = coefficients.coefficientsInReadableFormat();
            // write output to coefficientsFile

            try (FileWriter writer = new FileWriter(new File(coefficientsFile))) {
                writer.write(output);
            } catch (Exception e) {
                System.out.println("Error writing file " + coefficientsFile + " : " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
