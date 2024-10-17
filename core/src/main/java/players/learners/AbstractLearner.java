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
        WIN(3, false),  // 0 or 1 for loss/win
        ORDINAL(2, false), // -1 for first to -n for nth place
        SCORE(1, false),  // raw score
        SCORE_DELTA(1, false),  // targets the change in score between now and end of the game
        WIN_MEAN(3, true), // 0 or 1 for loss/win, with discount to 0.5 average (discount over the rounds in a game,
        // so that at the start (with little information), we reduce noise
        ORD_MEAN(2, true),  // as ORDINAL, but discounted to middle of the range based on rounds to final result
        ORD_SCALE(2, false), // as ORDINAL, but scaled to 0 to 1 (for Logistic regression targeting)
        ORD_MEAN_SCALE(2, true), // as ORD_MEAN, but scaled to 0 to 1 ( for Logistic regression targeting)
        ACTION_SCORE(4, false); // targets the score of the action taken (for Q-learning

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
                descriptions = new String[header.length - 11];
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
        // these fields are all defined in FeatureListener, and we assume that any Listener that records data
        // subclasses this class; adding additional game-specific data on top (or rather, in between).
        // TODO: This is unclear, and should be tidied up to reduce fragility
        if (!header[0].equals("GameID") || !header[1].equals("Player") || !header[2].equals("Round") || !header[3].equals("Turn") || !header[4].equals("CurrentScore")) {
            throw new AssertionError("Unexpected starting header entries " + String.join("", header));
        }
        if (!header[header.length - 1].equals("FinalScore") || !header[header.length - 2].equals("Ordinal")
                || !header[header.length - 3].equals("Win") || !header[header.length - 4].equals(("ActionScore"))
                || !header[header.length - 5].equals("TotalRounds") || !header[header.length - 6].equals("PlayerCount")) {
            throw new AssertionError("Unexpected final header entries " + String.join("", header));
        }

        dataArray = new double[data.size()][];
        target = new double[data.size()][1];
        currentScore = new double[data.size()][1];
        for (int i = 0; i < dataArray.length; i++) {
            double[] allData = data.get(i);
            // calculate the number of turns from this point until the end of the game
            double turns = allData[header.length - 5] - allData[2];
            double playerCount = allData[header.length - 6];
            // discount target (towards expected result where relevant)
            double expectedAverage = 0.0;
            if (targetType == Target.WIN_MEAN)
                expectedAverage = 1.0 / playerCount;
            if (targetType == Target.ORD_MEAN || targetType == Target.ORD_MEAN_SCALE)
                expectedAverage = (1.0 + playerCount) / 2.0;

            if (targetType == Target.SCORE_DELTA)
                target[i][0] = (allData[header.length - targetType.indexOffset] - allData[4]) * Math.pow(gamma, turns);
            else
                target[i][0] = (allData[header.length - targetType.indexOffset] - expectedAverage) * Math.pow(gamma, turns) + expectedAverage;

            if (targetType == Target.ORDINAL || targetType == Target.ORD_MEAN)
                target[i][0] = -target[i][0];  // if we are targeting the Ordinal position, then high is bad!
            if (targetType == Target.ORD_MEAN_SCALE || targetType == Target.ORD_SCALE)
                target[i][0] = (playerCount - target[i][0]) / (playerCount - 1.0);  // scale to [0, 1]

            currentScore[i][0] = allData[4];
            double[] regressionData = new double[header.length - 10];
            regressionData[0] = 1.0; // the bias term
            System.arraycopy(allData, 5, regressionData, 1, regressionData.length - 1);
            dataArray[i] = regressionData;
        }
    }

}
