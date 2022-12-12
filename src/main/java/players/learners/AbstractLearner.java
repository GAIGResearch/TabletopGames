package players.learners;

import core.interfaces.ILearner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractLearner implements ILearner {

    protected double[][] dataArray;
    protected String[] header;
    protected double[][] target;
    protected double[][] currentScore;
    String[] descriptions;
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

}
