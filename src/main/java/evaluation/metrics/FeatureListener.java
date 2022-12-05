package evaluation.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import core.interfaces.IStatisticLogger;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public abstract class FeatureListener extends GameListener {

    List<StateFeatureListener.LocalDataWrapper> currentData = new ArrayList<>();
    Event.GameEvent frequency;
    boolean currentPlayerOnly = false;

    protected FeatureListener(IStatisticLogger logger, Event.GameEvent frequency, boolean currentPlayerOnly) {
        super(logger, new AbstractMetric[]{});
        this.currentPlayerOnly = currentPlayerOnly;
        this.frequency = frequency;
    }

    @Override
    public void onEvent(Event event)
    {
        if(event.type == Event.GameEvent.GAME_OVER) {

            // first we record a final state for each player
            if(event.type == frequency)
                processFinalState(event.state, null);

            // now we can update the result
            int totP = event.state.getNPlayers();
            double[] finalScores = IntStream.range(0, totP).mapToDouble(event.state::getGameScore).toArray();
            double[] winLoss = Arrays.stream(event.state.getPlayerResults()).mapToDouble(r -> {
                switch (r) {
                    case WIN:
                        return 1.0;
                    case DRAW:
                        return 0.5;
                    default:
                        return 0.0;
                }
            }).toArray();
            double[] ordinal = IntStream.range(0, totP).mapToDouble(event.state::getOrdinalPosition).toArray();
            double finalRound = event.state.getTurnOrder().getRoundCounter();
            for (StateFeatureListener.LocalDataWrapper record : currentData) {
                // we use a LinkedHashMap so that the order of the keys is preserved, and hence the
                // data is written to file in a sensible order for human viewing
                Map<String, Double> data = new LinkedHashMap<>();
                data.put("GameID", (double) event.state.getGameID());
                data.put("Player", (double) record.player);
                data.put("Round", (double) record.gameRound);
                data.put("Turn", (double) record.gameTurn);
                data.put("CurrentScore", record.currentScore);
                for (int i = 0; i < record.array.length; i++) {
                    data.put(names()[i], record.array[i]);
                }
                data.put("PlayerCount", (double) game.getPlayers().size());
                data.put("TotalRounds", finalRound);
                data.put("Win", winLoss[record.player]);
                data.put("Ordinal", ordinal[record.player]);
                data.put("FinalScore", finalScores[record.player]);
                loggers.get(event.type).record(data);
            }
            loggers.get(event.type).processDataAndNotFinish();
            currentData = new ArrayList<>();
        }

    }

    public abstract String[] names();

    public abstract double[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

    public void setLogger(IStatisticLogger newLogger) {
        this.loggers = new HashMap<>();
        for (Event.GameEvent event: Event.GameEvent.values()) {
            this.loggers.put(event, newLogger.emptyCopy(event.name()));
        }
    }


    public void processFinalState(AbstractGameState state, AbstractAction action) {
        // we record one state for each player after every action is taken
        if (currentPlayerOnly && state.isNotTerminal()) {
            int p = state.getCurrentPlayer();
            double[] phi = extractFeatureVector(action, state, p);
            currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state));
        } else {
            for (int p = 0; p < state.getNPlayers(); p++) {
                double[] phi = extractFeatureVector(action, state, p);
                currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state));
            }
        }
    }

    // To avoid incessant boxing / unboxing if we were to use Double
    static class LocalDataWrapper {
        final int player;
        final int gameTurn;
        final int gameRound;
        final double currentScore;
        final double[] array;

        LocalDataWrapper(int player, double[] contents, AbstractGameState state) {
            array = contents;
            this.gameTurn = state.getTurnOrder().getTurnCounter();
            this.gameRound = state.getTurnOrder().getRoundCounter();
            this.player = player;
            this.currentScore = state.getGameScore(player);
        }
    }
}
