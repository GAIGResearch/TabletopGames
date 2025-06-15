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

    protected List<StateFeatureListener.LocalDataWrapper> currentData = new ArrayList<>();
    protected Event.GameEvent frequency;
    boolean currentPlayerOnly;
    protected IStatisticLogger logger;
    protected Game game;
    protected int everyN = 1;
    protected int currentRecordCount = 0;

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

    // Set the frequency of the events to be recorded; only every Nth event will be recorded
    public void setNth(int n) {
        this.everyN = n;
    }

    @Override
    public void onEvent(Event event) {

        if (event.type == frequency && frequency != Event.GameEvent.GAME_OVER) {
            // if GAME_OVER, then we cover this a few lines down

            // we only record every Nth event. This is to (optionally) generate sparser and less correlated data
            // If we record every event, then successive events are highly correlated (of course, sometimes we need the
            // complete trajectory)
            currentRecordCount++;
            if (currentRecordCount % everyN != 0) {
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

    protected void writeDataWithStandardHeaders(AbstractGameState state) {
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
            if (record.array.length > 0) {
                for (int i = 0; i < record.array.length; i++) {
                    data.put(names()[i], record.array[i]);
                }
            } else {
                for (int i = 0; i < record.objArray.length; i++) {
                    data.put(names()[i], record.objArray[i]);
                }
            }
            data.put("PlayerCount", getGame().getPlayers().size());
            data.put("TotalRounds", finalRound);
            data.put("TotalTurns", state.getTurnCounter());
            data.put("TotalTicks", state.getGameTick());
            for (int i = 0; i < record.actionScores.length; i++) {
                data.put(record.actionScoreNames[i], record.actionScores[i]);
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

    public abstract String[] names();

    public abstract double[] extractDoubleVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

    /*
     * Override this if the feature vector is not all numeric
     */
    public abstract Object[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

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
                currentData.add(new StateFeatureListener.LocalDataWrapper(currentPlayer, doubleData, state, new HashMap<>()));
            } else {
                Object[] phi = extractFeatureVector(action, state, currentPlayer);
                currentData.add(new StateFeatureListener.LocalDataWrapper(currentPlayer, phi, state, new HashMap<>()));
            }
        } else {
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (isDouble) {
                    double[] phi = p == currentPlayer ? doubleData : extractDoubleVector(action, state, p);
                    currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state, new HashMap<>()));
                } else {
                    Object[] phi = extractFeatureVector(action, state, p);
                    currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state, new HashMap<>()));
                }
            }
        }
    }

    // To avoid incessant boxing / unboxing if we were to use Double
    protected static class LocalDataWrapper {
        final int player;
        final int gameTurn;
        final int gameRound;
        final double currentScore;
        final double[] actionScores;
        final String[] actionScoreNames;
        final double[] array;
        final Object[] objArray;

        LocalDataWrapper(int player, double[] contents, Object[] contentsObj, AbstractGameState state, Map<String, Double> actionScore) {
            array = contents;
            objArray = contentsObj;
            this.gameTurn = state.getTurnCounter();
            this.gameRound = state.getRoundCounter();
            this.player = player;
            this.currentScore = state.getGameScore(player);
            this.actionScores = new double[actionScore.size()];
            this.actionScoreNames = new String[actionScore.size()];
            int i = 0;
            for (String key : actionScore.keySet()) {
                actionScoreNames[i] = key;
                actionScores[i] = actionScore.get(key);
                i++;
            }
        }
        public LocalDataWrapper(int player, Object[] contents, AbstractGameState state, Map<String, Double> actionScore) {
            this(player, new double[0], contents, state, actionScore);
        }
        public LocalDataWrapper(int player, double[] contents, AbstractGameState state, Map<String, Double> actionScore) {
            this(player, contents, new Object[0], state, actionScore);
        }

    }
}
