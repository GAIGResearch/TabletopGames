package utilities;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStatisticLogger;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public abstract class FeatureListener implements IGameListener {


    List<StateFeatureListener.LocalDataWrapper> currentData = new ArrayList<>();
    IStatisticLogger logger;
    CoreConstants.GameEvents frequency;
    boolean currentPlayerOnly = false;

    protected FeatureListener(IStatisticLogger logger, CoreConstants.GameEvents frequency, boolean currentPlayerOnly) {
        this.logger = logger;
        this.currentPlayerOnly = currentPlayerOnly;
        this.frequency = frequency;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            // first we record a final state for each player
            onEvent(frequency, game.getGameState(), null);
            // now we can update the result
            int totP = game.getGameState().getNPlayers();
            double[] finalScores = IntStream.range(0, totP).mapToDouble(game.getGameState()::getGameScore).toArray();
            double[] winLoss = Arrays.stream(game.getGameState().getPlayerResults()).mapToDouble(r -> {
                switch(r) {
                    case WIN:
                        return 1.0;
                    case DRAW:
                        return 0.5;
                    default:
                        return 0.0;
                }
            }).toArray();
            double[] ordinal = IntStream.range(0, totP).mapToDouble(game.getGameState()::getOrdinalPosition).toArray();
            double finalRound = game.getGameState().getTurnOrder().getRoundCounter();

            for (StateFeatureListener.LocalDataWrapper record : currentData) {
                // we use a LinkedHashMap so that the order of the keys is preserved, and hence the
                // data is written to file in a sensible order for human viewing
                Map<String, Double> data = new LinkedHashMap<>();
                data.put("GameID", (double) game.getGameState().getGameID());
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
                logger.record(data);
            }
            logger.processDataAndNotFinish();
            currentData = new ArrayList<>();
        }
    }

    public abstract String[] names();

    public abstract double[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer);

    public void setLogger(IStatisticLogger newLogger) {
        this.logger = newLogger;
    }
    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        // we record one state for each player after every action is taken
        if (type == frequency) {
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
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
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
