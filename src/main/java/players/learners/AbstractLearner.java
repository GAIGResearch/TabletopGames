package players.learners;

import core.interfaces.ILearner;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

public abstract class AbstractLearner implements ILearner {

    protected double[][] dataArray;
    protected String[] header;
    protected double[][] target;
    protected double[][] currentScore;
    String[] descriptions;
    double gamma;
    Target targetType;

    public enum Target {
        WIN("Win", false),  // 0 or 1 for loss/win
        ORDINAL("Ordinal", false), // -1 for first to -n for nth place
        SCORE("FinalScore", false),  // raw score
        SCORE_DELTA("FinalScoreAdv", false),  // targets the change in score between now and end of the game
        WIN_MEAN("Win", true), // 0 or 1 for loss/win, with discount to 0.5 average (discount over the rounds in a game,
        // so that at the start (with little information), we reduce noise
        ORD_MEAN("Ordinal", true),  // as ORDINAL, but discounted to middle of the range based on rounds to final result
        ORD_SCALE("Ordinal", false), // as ORDINAL, but scaled to 0 to 1 (for Logistic regression targeting)
        ORD_MEAN_SCALE("Ordinal", true), // as ORD_MEAN, but scaled to 0 to 1 ( for Logistic regression targeting)
        ACTION_SCORE("ActionScore", false); // targets the score of the action taken (for Q-learning

        public final boolean discountToMean;
        public final String header;

        Target(String header, boolean d) {
            this.header = header;
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

        Pair<List<String>, List<List<String>>> rawData = Utils.loadDataWithHeader("\t", files);
        List<double[]> data = rawData.b.stream()
                .map(s -> s.stream().mapToDouble(Double::parseDouble).toArray())
                .toList();
        header = rawData.a.toArray(new String[0]);

        String[] specialColumns = {"GameID", "Player", "Turn",  "CurrentScore", "Win", "Ordinal",
                "FinalScore", "FinalScoreAdv", "TotalRounds", "PlayerCount", "TotalTurns", "TotalTicks", "ActionScore"};
        Map<String, Integer> indexForSpecialColumns = new HashMap<>();

        // now link these to their actual index in the data
        for (int i = 0; i < header.length; i++) {
            String h = header[i];
            if (Arrays.asList(specialColumns).contains(h)) {
                indexForSpecialColumns.put(h, i);
            }
        }
        // then set descriptions to the rest of the data
        descriptions = new String[header.length - indexForSpecialColumns.size()];
        int j = 0;
        for (String h : header) {
            if (!indexForSpecialColumns.containsKey(h)) {
                descriptions[j] = h;
                j++;
            }
        }

        // TODO: discounting should really use TICKS as more reliably generic across games, even if it
        // does not map in the same way all the time

        dataArray = new double[data.size()][];
        target = new double[data.size()][1];
        currentScore = new double[data.size()][1];
        for (int i = 0; i < dataArray.length; i++) {
            double[] allData = data.get(i);
            // calculate the number of turns from this point until the end of the game
            double turns = allData[indexForSpecialColumns.get("TotalTurns")] - allData[indexForSpecialColumns.get("Turn")];
            double playerCount = allData[indexForSpecialColumns.get("PlayerCount")];
            // discount target (towards expected result where relevant)
            double expectedAverage = 0.0;
            if (targetType == Target.WIN_MEAN)
                expectedAverage = 1.0 / playerCount;
            if (targetType == Target.ORD_MEAN || targetType == Target.ORD_MEAN_SCALE)
                expectedAverage = (1.0 + playerCount) / 2.0;

            if (targetType == Target.SCORE_DELTA)
                target[i][0] = (allData[indexForSpecialColumns.get(targetType.header)] -
                        allData[indexForSpecialColumns.get("CurrentScore")]) * Math.pow(gamma, turns);
            else
                target[i][0] = (allData[indexForSpecialColumns.get(targetType.header)] - expectedAverage) * Math.pow(gamma, turns) + expectedAverage;

            if (targetType == Target.ORDINAL || targetType == Target.ORD_MEAN)
                target[i][0] = -target[i][0];  // if we are targeting the Ordinal position, then high is bad!
            if (targetType == Target.ORD_MEAN_SCALE || targetType == Target.ORD_SCALE)
                target[i][0] = (playerCount - target[i][0]) / (playerCount - 1.0);  // scale to [0, 1]

            currentScore[i][0] = allData[indexForSpecialColumns.get("CurrentScore")];
            double[] regressionData = new double[descriptions.length + 1];
            regressionData[0] = 1.0; // the bias term
            // then copy the rest of the data into the regression data
            // we skip the special columns (GameID, Player, Turn, CurrentScore, Win, Ordinal, FinalScore, FinalScoreAdv)
            // and just copy the rest of the data
            j = 1;
            for (int h = 0; h < header.length; h++) {
                String headerName = header[h];
                if (!indexForSpecialColumns.containsKey(headerName)) {
                    regressionData[j] = allData[h];
                    j++;
                }
            }
            dataArray[i] = regressionData;
        }
    }

}
