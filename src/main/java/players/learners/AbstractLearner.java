package players.learners;

import core.interfaces.IActionFeatureVector;
import core.interfaces.ILearner;
import core.interfaces.IStateFeatureVector;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static java.util.stream.Collectors.toList;

public abstract class AbstractLearner implements ILearner {

    protected double[][] dataArray;
    protected String[] header;
    protected double[][] target;
    protected double[][] currentScore;
    String[] descriptions;
    double gamma;
    Target targetType;
    IStateFeatureVector stateFeatureVector;
    IActionFeatureVector actionFeatureVector;

    public enum Target {
        WIN("Win", false),  // 0 or 1 for loss/win
        ORDINAL("Ordinal", false), // -1 for first to -n for nth place
        SCORE("FinalScore", false),  // raw score
        SCORE_DELTA("FinalScoreAdv", false),  // targets the difference between final score and that of the best other player
        WIN_MEAN("Win", true), // 0 or 1 for loss/win, with discount to 0.5 average (discount over the rounds in a game,
        // so that at the start (with little information), we reduce noise
        ORD_MEAN("Ordinal", true),  // as ORDINAL, but discounted to middle of the range based on rounds to final result
        ORD_SCALE("Ordinal", false), // as ORDINAL, but scaled to 0 to 1 (for Logistic regression targeting)
        ORD_MEAN_SCALE("Ordinal", true), // as ORD_MEAN, but scaled to 0 to 1 ( for Logistic regression targeting)
        ACTION_CHOSEN("CHOSEN", false), // targets the probability of the action taken
        ACTION_VISITS("VISIT_PROPORTION", false),
        ACTION_ADV("ADVANTAGE", false), // targets the advantage of the action taken
        ACTION_SCORE("ACTION_VALUE", false); // targets the score of the action taken (for Q-learning

        public final boolean discountToMean;
        public final String header;

        Target(String header, boolean d) {
            this.header = header;
            discountToMean = d;
        }
    }

    public AbstractLearner(IStateFeatureVector stateFeatureVector) {
        this(1.0, Target.WIN, stateFeatureVector);
    }

    public AbstractLearner(IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        this(1.0, Target.ACTION_SCORE, stateFeatureVector, actionFeatureVector);
    }

    public AbstractLearner(){}

    public AbstractLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector) {
        this.gamma = gamma;
        this.targetType = target;
        this.stateFeatureVector = stateFeatureVector;
    }

    public AbstractLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        this.gamma = gamma;
        this.targetType = target;
        this.stateFeatureVector = stateFeatureVector;
        this.actionFeatureVector = actionFeatureVector;
    }

    public AbstractLearner setStateFeatureVector(IStateFeatureVector stateFeatureVector) {
        this.stateFeatureVector = stateFeatureVector;
        return this;
    }

    public AbstractLearner setActionFeatureVector(IActionFeatureVector actionFeatureVector) {
        this.actionFeatureVector = actionFeatureVector;
        return this;
    }

    public IActionFeatureVector getActionFeatureVector() {
        return actionFeatureVector;
    }
    public IStateFeatureVector getStateFeatureVector() {
        return stateFeatureVector;
    }

    public AbstractLearner setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }
    public AbstractLearner setTarget(Target target) {
        this.targetType = target;
        return this;
    }

    public int featureCount() {
        int stateCount = stateFeatureVector == null ? 0 : stateFeatureVector.names().length;
        int actionCount = actionFeatureVector == null ? 0 : actionFeatureVector.names().length;
        return stateCount + actionCount;
    }

    protected void loadData(String... files) {

        Pair<List<String>, List<List<String>>> rawData = Utils.loadDataWithHeader("\t", files);
        header = rawData.a.toArray(new String[0]);

        String[] specialColumns = {"GameID", "Player", "Turn", "Round", "Tick", "CurrentScore", "Win", "Ordinal",
                "FinalScore", "FinalScoreAdv", "TotalRounds", "PlayerCount", "TotalTurns", "TotalTicks",
                "ActualWin", "ActualOrdinal", "ActualScore", "ActualScoreAdv",
                "CHOSEN", "ACTION_VISITS", "ADVANTAGE", "ACTION_VALUE", "VISIT_PROPORTION"};
        Map<String, Integer> indexForSpecialColumns = new HashMap<>();

        // then set descriptions to the rest of the data
        // and validate that the data matches the feature vector
        descriptions = stateFeatureVector == null ?
                actionFeatureVector.names() : stateFeatureVector.names();
        List<String> expectedNames = Arrays.stream(descriptions).collect(toList());
        Map<String, Integer> indexForDescriptions = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String h = header[i];
//            if (!indexForSpecialColumns.containsKey(h) && !expectedNames.contains(h)) {
//                System.out.println("Unexpected feature: " + h);
//            }
            if (expectedNames.contains(h)) {
                indexForDescriptions.put(h, i);
                expectedNames.remove(h);
            }
            if (Arrays.asList(specialColumns).contains(h)) {
                indexForSpecialColumns.put(h, i);
            }
        }
        // we allow missing features in the data, but not extra ones
        if (!expectedNames.isEmpty()) {
            System.out.println("Missing features: " + expectedNames);
        }

        // TODO: discounting should really use TICKS as more reliably generic across games, even if it
        // does not map in the same way all the time

        dataArray = new double[rawData.b.size()][];
        target = new double[rawData.b.size()][1];
        currentScore = new double[rawData.b.size()][1];
        for (int i = 0; i < dataArray.length; i++) {
            List<String> allData = rawData.b.get(i);
            // calculate the number of turns from this point until the end of the game
            double turns = Double.parseDouble(allData.get(indexForSpecialColumns.get("TotalTurns"))) -
                    Double.parseDouble(allData.get(indexForSpecialColumns.get("Turn")));
            double playerCount = Double.parseDouble(allData.get(indexForSpecialColumns.get("PlayerCount")));
            int targetIndex = indexForSpecialColumns.getOrDefault(targetType.header, -1);
            if (targetIndex == -1) {
                throw new IllegalArgumentException("Target " + targetType.header + " not found in data");
            }
            // discount target (towards expected result where relevant)
            double expectedAverage = 0.0;
            if (targetType == Target.WIN_MEAN)
                expectedAverage = 1.0 / playerCount;
            if (targetType == Target.ORD_MEAN || targetType == Target.ORD_MEAN_SCALE)
                expectedAverage = (1.0 + playerCount) / 2.0;

            if (targetType == Target.SCORE_DELTA)
                target[i][0] = Double.parseDouble(allData.get(targetIndex)) * Math.pow(gamma, turns);
            else {
                target[i][0] = (Double.parseDouble(allData.get(targetIndex)) - expectedAverage) * Math.pow(gamma, turns) + expectedAverage;
            }

            if (targetType == Target.ORDINAL || targetType == Target.ORD_MEAN)
                target[i][0] = -target[i][0];  // if we are targeting the Ordinal position, then high is bad!
            if (targetType == Target.ORD_MEAN_SCALE || targetType == Target.ORD_SCALE)
                target[i][0] = (playerCount - target[i][0]) / (playerCount - 1.0);  // scale to [0, 1]

            currentScore[i][0] = Double.parseDouble(allData.get(indexForSpecialColumns.get("CurrentScore")));
            double[] regressionData = new double[descriptions.length + 1];
            regressionData[0] = 1.0; // the bias term
            // then copy the rest of the data into the regression data
            // the order of the data in the regression data is the same as the order in the names() of the feature vector
            int j = 1;
            for (String h : descriptions) {
                if (indexForDescriptions.get(h) != null) {
                    regressionData[j] = Double.parseDouble(allData.get(indexForDescriptions.get(h)));
                    j++;
                }
            }
            dataArray[i] = regressionData;
        }
    }

}
