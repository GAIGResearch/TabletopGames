package evaluation.metrics;
import core.Game;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import utilities.Pair;
import utilities.TAGStatSummary;
import utilities.TAGSummariser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public enum EndGameStatisticsAttributes implements IGameMetric {
    ACTION_SPACE((l,e) ->
    {
        List<Pair<Integer, Integer>> actionSpaceRecord = e.game.getActionSpaceSize();
        TAGStatSummary stats = actionSpaceRecord.stream()
                .map(r -> r.b)
                .filter(size -> size > 1)
                .collect(new TAGSummariser());
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("ActionSpaceMean", stats.mean());
        collectedData.put("ActionSpaceMin", stats.min());
        collectedData.put("ActionSpaceMedian", stats.median());
        collectedData.put("ActionSpaceMax", stats.max());
        collectedData.put("ActionSpaceSkew", stats.skew());
        collectedData.put("ActionSpaceKurtosis", stats.kurtosis());
        collectedData.put("ActionSpaceVarCoeff", Math.abs(stats.sd() / stats.mean()));
        return collectedData;
    }),
    TIMES((l,e) ->
    {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("TimeNext", e.game.getNextTime() / 1e3);
        collectedData.put("TimeCopy", e.game.getCopyTime() / 1e3);
        collectedData.put("TimeActionCompute", e.game.getActionComputeTime() / 1e3);
        collectedData.put("TimeAgent", e.game.getAgentTime() / 1e3);
        return collectedData;
    }),
    DURATION((l,e) -> {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("Turns", e.game.getGameState().getTurnOrder().getTurnCounter());
        collectedData.put("Ticks", e.game.getTick());
        collectedData.put("Rounds", e.game.getGameState().getTurnOrder().getRoundCounter());
        return collectedData;
    }),
    DECISIONS((l,e) ->
    {
        List<Pair<Integer, Integer>> actionSpaceRecord = e.game.getActionSpaceSize();
        TAGStatSummary stats = actionSpaceRecord.stream()
                .map(r -> r.b)
                .filter(size -> size > 1)
                .collect(new TAGSummariser());
        Map<String, Object> collectedData = new LinkedHashMap<>();
        List<Integer> decisionPoints = l.getDecisionPoints();
        collectedData.put("ActionsPerTurnSum", e.game.getNActionsPerTurn());
        TAGStatSummary movesWithDecision = decisionPoints.stream().collect(new TAGSummariser());
        collectedData.put("Decisions", stats.n());
        collectedData.put("DecisionPointsMean", movesWithDecision.mean());
        return collectedData;

    }),
    SCORE((l, e) ->
    {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        List<Double> scores = l.getScores();
        TAGStatSummary sc = scores.stream().collect(new TAGSummariser());
        collectedData.put("ScoreMedian", sc.median());
        collectedData.put("ScoreMean", sc.mean());
        collectedData.put("ScoreMax", sc.max());
        collectedData.put("ScoreMin", sc.min());
        collectedData.put("ScoreVarCoeff", Math.abs(sc.sd() / sc.mean()));
        TAGStatSummary scoreDelta = scores.size() > 1 ?
                IntStream.range(0, scores.size() - 1)
                        .mapToObj(i -> !scores.get(i + 1).equals(scores.get(i)) ? 1.0 : 0.0)
                        .collect(new TAGSummariser())
                : new TAGStatSummary();
        collectedData.put("ScoreDelta", scoreDelta.mean()); // percentage of actions that lead to a change in score
        return collectedData;
    }),
    STATE_SIZE((l, e) ->
    {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        List<Integer> components = l.getComponents();
        TAGStatSummary stateSize = components.stream().collect(new TAGSummariser());
        collectedData.put("StateSizeMedian", stateSize.median());
        collectedData.put("StateSizeMean", stateSize.mean());
        collectedData.put("StateSizeMax", stateSize.max());
        collectedData.put("StateSizeMin", stateSize.min());
        collectedData.put("StateSizeVarCoeff", Math.abs(stateSize.sd() / stateSize.mean()));
        return collectedData;
    }),
    VISIBILITY((l, e) -> {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        List<Double> visibilityOnTurn = l.getVisibility();
        TAGStatSummary visibility = visibilityOnTurn.stream().collect(new TAGSummariser());
        collectedData.put("HiddenInfoMedian", visibility.median());
        collectedData.put("HiddenInfoMean", visibility.mean());
        collectedData.put("HiddenInfoMax", visibility.max());
        collectedData.put("HiddenInfoMin", visibility.min());
        collectedData.put("HiddenInfoVarCoeff", Math.abs(visibility.sd() / visibility.mean()));
        return collectedData;
    });
    private final BiFunction<GameStatisticsListener, Event, Object> lambda;
    EndGameStatisticsAttributes(BiFunction<GameStatisticsListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    public Object get(GameListener listener, Event event)
    {
        return lambda.apply((GameStatisticsListener)listener, event);
    }

}
