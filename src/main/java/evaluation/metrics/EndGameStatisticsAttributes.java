package evaluation.metrics;
import core.interfaces.IGameMetric;
import utilities.Pair;
import evaluation.summarisers.TAGStatSummary;
import evaluation.summarisers.TAGSummariser;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public enum EndGameStatisticsAttributes implements IGameMetric {
    TIMES((l,e) ->
    {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("TimeNext", l.getGame().getNextTime() / 1e3);
        collectedData.put("TimeCopy", l.getGame().getCopyTime() / 1e3);
        collectedData.put("TimeActionCompute", l.getGame().getActionComputeTime() / 1e3);
        collectedData.put("TimeAgent", l.getGame().getAgentTime() / 1e3);
        return collectedData;
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }}),
    DURATION((l,e) -> {
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("Turns", l.getGame().getGameState().getTurnOrder().getTurnCounter());
        collectedData.put("Ticks", l.getGame().getTick());
        collectedData.put("Rounds", l.getGame().getGameState().getTurnOrder().getRoundCounter());
        return collectedData;
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }}),
    ORDINAL((l,e) -> e.state.getOrdinalPosition(e.playerID), new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }}, true),
    PLAYER_TYPE((l,e) -> l.game.getPlayers().get(e.playerID).toString(), new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }}, true),
    DECISIONS((l,e) ->
    {
        List<Pair<Integer, Integer>> actionSpaceRecord = l.getGame().getActionSpaceSize();
        TAGStatSummary statsDecisionsAll = actionSpaceRecord.stream()
                .map(r -> r.b)
                .collect(new TAGSummariser());
        TAGStatSummary statsDecisions = actionSpaceRecord.stream()
                .map(r -> r.b)
                .filter(size -> size > 1)
                .collect(new TAGSummariser());
        Map<String, Object> collectedData = new LinkedHashMap<>();

        collectedData.put("ActionsPerTurnSum", l.getGame().getNActionsPerTurn());
        collectedData.put("Decisions", statsDecisions.n());
        collectedData.put("DecisionPointsMean", statsDecisions.n() * 1.0 / statsDecisionsAll.n());
        return collectedData;
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }});

    private final BiFunction<GameListener, Event, Object> lambda;
    private final HashSet<Event.GameEvent> eventTypes;
    private final boolean recordedPerPlayer;

    EndGameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda, boolean recordedPerPlayer) {
        this.lambda = lambda;
        this.eventTypes = null;
        this.recordedPerPlayer = recordedPerPlayer;
    }

    EndGameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
        this.eventTypes = null;
        this.recordedPerPlayer = false;
    }

    EndGameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda,  HashSet<Event.GameEvent> events) {
        this.lambda = lambda;
        this.eventTypes = events;
        this.recordedPerPlayer = false;
    }

    EndGameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda,  HashSet<Event.GameEvent> events, boolean recordedPerPlayer) {
        this.lambda = lambda;
        this.eventTypes = events;
        this.recordedPerPlayer = recordedPerPlayer;
    }

    public Object get(GameListener listener, Event event)
    {
        return lambda.apply(listener, event);
    }

    public boolean listens(Event.GameEvent eventType)
    {
        if(eventTypes == null) return true; //by default, we listen to all types.
        return eventTypes.contains(eventType);
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return recordedPerPlayer;
    }
}
