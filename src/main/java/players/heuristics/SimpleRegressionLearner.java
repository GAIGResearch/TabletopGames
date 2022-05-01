package players.heuristics;

import core.interfaces.ILearner;
import core.interfaces.IStateFeatureVector;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class SimpleRegressionLearner implements ILearner {

    @Override
    public void learnFrom(String... files) {
        // not yet implemented
    }

    @Override
    public boolean writeToFile(String file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("Some test data");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


}
