package players.learners;

import core.interfaces.ILearner;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractLearner implements ILearner {

    protected double[][] dataArray;
    protected String[] header;
    protected double[][] target;
    protected double[][] currentScore;
    protected ArrayList<Attribute> attributes;
    protected boolean addNoise = false;
    protected double noiseLevel = 0.01;
    String[] descriptions;
    private final Random rnd = new Random(System.currentTimeMillis());
    double gamma;
    Target targetType;

    public enum Target {
        WIN(3, false), ORDINAL(2, false), SCORE(1, false), SCORE_DELTA(1, false),
        WIN_MEAN(3, true), ORD_MEAN(2, true);
        public final int indexOffset;
        public final boolean discountToMean;

        Target(int i, boolean d) {
            indexOffset = i;
            discountToMean = d;
        }
    }

    public AbstractLearner() {
        this(1.0, Target.WIN);
    }

    public AbstractLearner(double gamma, Target target) {
        this.gamma = gamma;
        this.targetType = target;
    }

    public void setGamma(double newGamma) {
        gamma = newGamma;
    }

    public void setTarget(Target newTarget) {
        targetType = newTarget;
    }

    protected void loadData(String... files) {
        List<double[]> data = new ArrayList<>();
        for (String file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                header = reader.readLine().split("\\t");
                descriptions = new String[header.length - 10];
                System.arraycopy(header, 5, descriptions, 0, descriptions.length);
                while (reader.ready()) {
                    double[] datum = Arrays.stream(reader.readLine().split("\\t")).mapToDouble(Double::parseDouble).toArray();
                    data.add(datum);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError("Problem reading file " + file);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new AssertionError("Problem parsing data as numeric : " + file);
            }
        }

        // now convert data to [][]
        // we assume (for the moment) that the columns are: GameID, Player, Round, Turn, CurrentScore... Win, Ordinal, FinalScore
        // with ... representing the game specific features
        if (!header[0].equals("GameID") || !header[1].equals("Player") || !header[2].equals("Round") || !header[3].equals("Turn") || !header[4].equals("CurrentScore")) {
            throw new AssertionError("Unexpected starting header entries " + String.join("", header));
        }
        if (!header[header.length - 1].equals("FinalScore") || !header[header.length - 2].equals("Ordinal")
                || !header[header.length - 3].equals("Win") || !header[header.length - 4].equals("TotalRounds")
                || !header[header.length - 5].equals("PlayerCount")) {
            throw new AssertionError("Unexpected final header entries " + String.join("", header));
        }
        attributes = new ArrayList<>();
        attributes.add(new Attribute("BIAS"));
        for (int i = 5; i < header.length - 5; i++)
            attributes.add(new Attribute(header[i]));
        dataArray = new double[data.size()][];
        target = new double[data.size()][1];
        currentScore = new double[data.size()][1];
        for (int i = 0; i < dataArray.length; i++) {
            double[] allData = data.get(i);
            // calculate the number of turns from this point until the end of the game
            double turns = allData[header.length - 4] - allData[2];
            // discount target (towards expected result where relevant)
            double expectedAverage = 0.0;
            if (targetType == Target.WIN_MEAN)
                expectedAverage = 1.0 / allData[header.length - 5];
            if (targetType == Target.ORD_MEAN)
                expectedAverage = (1.0 + allData[header.length - 5]) / 2.0;
            if (targetType == Target.SCORE_DELTA)
                target[i][0] = (allData[header.length - targetType.indexOffset] - allData[4]) * Math.pow(gamma, turns);
            else
                target[i][0] = (allData[header.length - targetType.indexOffset] - expectedAverage) * Math.pow(gamma, turns) + expectedAverage;
            if (targetType == Target.ORDINAL || targetType == Target.ORD_MEAN)
                target[i][0] = -target[i][0];  // if we are targeting the Ordinal position, then high is bad!
            currentScore[i][0] = allData[4];
            double[] regressionData = new double[header.length - 9];
            regressionData[0] = 1.0; // the bias term
            System.arraycopy(allData, 5, regressionData, 1, regressionData.length - 1);
            dataArray[i] = regressionData;
        }
    }

    protected Instances createInstances(boolean includeBias) {
        List<String> values = new ArrayList<>();
        values.add("0");
        values.add("1");
        attributes.add(new Attribute("Win", values));
        Instances dataInstances = new Instances("data", attributes, dataArray.length - 1);
        if (!includeBias)
            attributes.remove(0);
        for (int i = 0; i < dataArray.length; i++) {
            double[] record = new double[dataArray[i].length - 1];
            System.arraycopy(dataArray[i], includeBias ? 0 : 1, record, 0, record.length);
            // we may skip the bias term in dataArray at position 0
            if (addNoise)
                for (int j = 1; j < record.length; j++)  // we do not add noise to the BIAS term
                    record[j] += rnd.nextDouble() * noiseLevel; // to avoid WEKA removing
            double[] XandY = new double[record.length + 1];
            System.arraycopy(record, 0, XandY, 0, record.length);
            XandY[record.length] = 1.0 - target[i][0]; // this puts the first category (0) for a win, and the second (1) as a loss.
            // this means that we learn a classifier to identify wins, and the coefficients are more naturally interpretable
            dataInstances.add(new DenseInstance(1.0, XandY));
        }
        dataInstances.setClassIndex(attributes.size() - 1);
        return dataInstances;
    }
}
