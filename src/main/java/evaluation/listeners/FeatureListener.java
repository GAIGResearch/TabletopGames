package evaluation.listeners;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public abstract class FeatureListener implements IGameListener {

    // currentData is the data specified by the feature vector
    // overrideData is then used to override the final values recorded at the end of the game
    protected List<StateFeatureListener.LocalDataWrapper> currentData = new ArrayList<>();
    protected Event.GameEvent frequency;
    boolean currentPlayerOnly;
    protected IStatisticLogger logger;
    protected Game game;
    protected double sampleRate = 1.0; // what proportion of events to record
    protected Random rnd = new Random();

    protected FeatureListener(Event.GameEvent frequency, boolean currentPlayerOnly) {
        this.currentPlayerOnly = currentPlayerOnly;
        this.frequency = frequency;
    }

    public void setLogger(IStatisticLogger logger) {
        if (logger != null) {
            logger.processDataAndFinish();
        }
        this.logger = logger;
    }

    public void setSampleRate(double rate) {
        if (rate <= 0 || rate > 1.0) throw new IllegalArgumentException("Sample rate must be in the range (0,1]");
        sampleRate = rate;
    }

    @Override
    public void onEvent(Event event) {

        if (event.type == frequency && frequency != Event.GameEvent.GAME_OVER) {
            // if GAME_OVER, then we cover this a few lines down

            // we only sample rare events. This is to (optionally) generate sparser and less correlated data
            // If we record every event, then successive events are highly correlated (of course, sometimes we need the
            // complete trajectory)
            if (rnd.nextDouble() > sampleRate) {
                return;
            }
            processState(event.state, event.action);
        }

        if (event.type == Event.GameEvent.GAME_OVER) {
            // first we record a final state for each player
            processState(event.state, null);

            // now we can update the result
            writeDataWithStandardHeaders(event.state);
        }
    }

    @Override
    public boolean setOutputDirectory(String... nestedDirectories) {
        if (logger instanceof FileStatsLogger fileLogger) {
            fileLogger.setOutPutDirectory(nestedDirectories);
        }
        return true;
    }

    public void writeDataWithStandardHeaders(AbstractGameState state) {
        int totP = state.getNPlayers();
        double[] finalScores = IntStream.range(0, totP).mapToDouble(state::getGameScore).toArray();
        double[] winLoss = Arrays.stream(state.getPlayerResults()).mapToDouble(r -> switch (r) {
            case WIN_GAME -> 1.0;
            case DRAW_GAME -> 0.5;
            default -> 0.0;
        }).toArray();
        double[] ordinal = IntStream.range(0, totP).mapToDouble(state::getOrdinalPosition).toArray();
        double finalRound = state.getRoundCounter();

        for (StateFeatureListener.LocalDataWrapper record : currentData) {
            // we use a LinkedHashMap so that the order of the keys is preserved, and hence the
            // data is written to file in a sensible order for human viewing
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("GameID", state.getGameID());
            data.put("Player", record.player);
            data.put("Round", record.gameRound);
            data.put("Turn", record.gameTurn);
            data.put("CurrentScore", record.currentScore);
            for (String key : record.values.keySet()) {
                data.put(key, record.values.get(key));
            }
            data.put("PlayerCount", getGame().getPlayers().size());
            data.put("TotalRounds", finalRound);
            data.put("TotalTurns", state.getTurnCounter());
            data.put("TotalTicks", state.getGameTick());
            for (String actionKey : record.actionValues.keySet()) {
                data.put(actionKey, record.actionValues.get(actionKey));
            }
            // We record the actual results of the game. If the sub-class Listener has not
            // set the corresponding Target fields (Win, Ordinal, FinalScore, FinalScoreAdv), then
            // we set these to default to the actual end game values.

            data.put("ActualWin", winLoss[record.player]);
            if (!data.containsKey("Win")) {
                data.put("Win", winLoss[record.player]);
            }
            data.put("ActualOrdinal", ordinal[record.player]);
            if (!data.containsKey("Ordinal")) {
                data.put("Ordinal", ordinal[record.player]);
            }
            data.put("ActualScore", finalScores[record.player]);
            if (!data.containsKey("FinalScore")) {
                data.put("FinalScore", finalScores[record.player]);
            }
            double bestOtherScore = IntStream.range(0, totP)
                    .filter(p -> p != record.player)
                    .mapToDouble(i -> finalScores[i])
                    .max().orElse(0);
            data.put("ActualScoreAdv", finalScores[record.player] - bestOtherScore);
            if (!data.containsKey("FinalScoreAdv")) {
                data.put("FinalScoreAdv", finalScores[record.player] - bestOtherScore);
            }
            logger.record(data);
        }
        logger.processDataAndNotFinish();
        currentData = new ArrayList<>();
    }

    @Override
    public void report() {
        logger.processDataAndFinish();
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    public abstract double[] extractDoubleVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

    /*
     * Override this if the feature vector is not all numeric
     */
    public abstract Object[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

    public abstract String[] names();

    public void processState(AbstractGameState state, AbstractAction action) {
        // we record one state for each player after each relevant event occurs
        // we first determine if the data is double[] or Object[]
        boolean isDouble = true;
        int currentPlayer = state.getCurrentPlayer();
        double[] doubleData = new double[0];
        try {
            doubleData = extractDoubleVector(action, state, currentPlayer);
        } catch (UnsupportedOperationException e) {
            isDouble = false;
        }
        if (currentPlayerOnly && state.isNotTerminal()) {
            if (isDouble) {
                currentData.add(LocalDataWrapper.factory(currentPlayer, doubleData, names(), state, new HashMap<>()));
            } else {
                Object[] phi = extractFeatureVector(action, state, currentPlayer);
                currentData.add(LocalDataWrapper.factory(currentPlayer, phi, names(), state, new HashMap<>()));
            }
        } else {
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (isDouble) {
                    double[] phi = p == currentPlayer ? doubleData : extractDoubleVector(action, state, p);
                    currentData.add(LocalDataWrapper.factory(p, phi, names(), state, new HashMap<>()));
                } else {
                    Object[] phi = extractFeatureVector(action, state, p);
                    currentData.add(LocalDataWrapper.factory(p, phi, names(), state, new HashMap<>()));
                }
            }
        }
    }

    public void addValueToLastRecord(String key, Object value) {
        if (currentData.isEmpty()) {
            throw new IllegalStateException("No records available to add value to. Ensure processState has been called before adding values.");
        }
        StateFeatureListener.LocalDataWrapper lastRecord = currentData.get(currentData.size() - 1);
        lastRecord.addValue(key, value);
    }

    // To avoid incessant boxing / unboxing if we were to use Double
    protected static class LocalDataWrapper {
        final int player;
        final int gameTurn;
        final int gameRound;
        final double currentScore;
        private final Map<String, Object> values;
        private final Map<String, Number> actionValues = new HashMap<>();

        LocalDataWrapper(int player, Map<String, Object> data, AbstractGameState state, Map<String, Number> actionScore) {
            values = new LinkedHashMap<>(data);
            this.gameTurn = state.getTurnCounter();
            this.gameRound = state.getRoundCounter();
            this.player = player;
            this.currentScore = state.getGameScore(player);
            if (actionScore != null) {
                this.actionValues.putAll(actionScore);
            }
        }

        public void addValue(String key, Object value) {
            values.put(key, value);
        }

        public void getValue(String key) {
            values.get(key);
        }

        public static LocalDataWrapper factory(int player, double[] contents, String[] names, AbstractGameState state, Map<String, Number> actionScore) {
            Object[] values = new Object[contents.length];
            for (int i = 0; i < contents.length; i++) {
                values[i] = contents[i];
            }
            return factory(player, values, names, state, actionScore);
        }

        public static LocalDataWrapper factory(int player, Object[] contents, String[] names, AbstractGameState state, Map<String, Number> actionScore) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < contents.length; i++) {
                values.put(names[i], contents[i]);
            }
            return new LocalDataWrapper(player, values, state, actionScore);
        }

    }
}
