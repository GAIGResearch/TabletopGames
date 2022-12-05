package evaluation.metrics;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IComponentContainer;
import core.interfaces.IGameMetric;
import utilities.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.*;
import java.util.function.BiFunction;

public enum GameStatisticsAttributes implements IGameMetric {
    GAME_START((l, e) ->
    {
        Game game = l.getGame();
        AbstractGameState state = game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        Map<String, Object> collectedData = new HashMap<>();
        collectedData.put("Game", game.getGameState().getGameType().name());
        collectedData.put("GameID", game.getGameState().getGameID());
        collectedData.put("Players", String.valueOf(game.getGameState().getNPlayers()));
        long s = System.nanoTime();
        fm.setup(state);
        long postS = System.nanoTime() - s;
        collectedData.put("TimeSetup", postS / 1e3);

        Pair<Integer, int[]> components = countComponents(state);
        collectedData.put("AvgHiddenInfo", Arrays.stream(components.b).sum() / (double) components.a / state.getNPlayers());

        return collectedData;
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ABOUT_TO_START);
    }}),
    SEED((l, e) -> e.state.getGameParameters().getRandomSeed(), new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ABOUT_TO_START);
    }}),
    GAME_STATUS((l, e) -> e.state.getGameStatus(), new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.GAME_OVER);
    }}),
    SCORE((l, e) ->
    {
        int player = e.state.getCurrentPlayer();
        return e.state.getGameScore(player);
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ACTION_CHOSEN);
    }}),
    ACTION_SPACE((l, e) ->
    {
        AbstractForwardModel fm = l.getGame().getForwardModel();
        return fm.computeAvailableActions(l.getGame().getGameState()).size();
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ACTION_CHOSEN);
    }}),
    STATE_SIZE ((l, e) -> {
        int components = countComponents(e.state).a;
        return (double) components;
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ACTION_CHOSEN);
        add(Event.GameEvent.ABOUT_TO_START);
    }}),
    VISIBILITY_CURRENT_PLAYER((l, e) ->
    {
        AbstractGameState gs = e.state;
        int player = gs.getCurrentPlayer();
        Pair<Integer, int[]> allComp = countComponents(gs);
        return (allComp.b[player] / (double) allComp.a);
    }, new HashSet<Event.GameEvent>() {{
        add(Event.GameEvent.ACTION_CHOSEN);
    }});

    /**
     * Returns the total number of components in the state as the first element of the returned value
     * and an array of the counts that are hidden to each player
     * <p>
     *
     * @param state
     * @return The total number of components
     */
    private static Pair<Integer, int[]> countComponents(AbstractGameState state) {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        // we do not include containers in the count...just the lowest-level items
        // open to debate on this. But we are consistent across State Size and Hidden Information stats
        int total = (int) state.getAllComponents().stream().filter(c -> !(c instanceof IComponentContainer)).count();
        for (int p = 0; p < hiddenByPlayer.length; p++)
            hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }

    private final BiFunction<GameListener, Event, Object> lambda;
    private final HashSet<Event.GameEvent> eventTypes;

    GameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
        this.eventTypes = null;
    }

    GameStatisticsAttributes(BiFunction<GameListener, Event, Object> lambda, HashSet<Event.GameEvent> events) {
        this.lambda = lambda;
        this.eventTypes = events;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    public boolean listens(Event.GameEvent eventType)
    {
        if(eventTypes == null) return true; //by default, we listen to all types.
        return eventTypes.contains(eventType);
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }
}
