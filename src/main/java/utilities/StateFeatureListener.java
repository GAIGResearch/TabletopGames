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
public class StateFeatureListener implements IGameListener {

    IStateFeatureVector phiFn;
    List<DoubleArrayWrapper> currentData = new ArrayList<>();
    IStatisticLogger logger;

    public StateFeatureListener(IStatisticLogger logger, IStateFeatureVector phi) {
        this.phiFn = phi;
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            // now we can update the result
            int totP =  game.getGameState().getNPlayers();
            double[] finalScores = IntStream.range(0, totP).mapToDouble(game.getGameState()::getGameScore).toArray();
            double[] winLoss = Arrays.stream(game.getGameState().getPlayerResults()).mapToDouble(r -> r.value).toArray();
            double[] ordinal = IntStream.range(0, totP).mapToDouble(game.getGameState()::getOrdinalPosition).toArray();

            for (DoubleArrayWrapper record : currentData) {
                Map<String, Double> data = new LinkedHashMap<>();
                // we use a LinkedHashMap so that the order of the keys is preserved, and hence the
                // data is written to file in a sensible order for human viewing
                for (int i = 0; i < record.array.length; i++) {
                    data.put(phiFn.names()[i], record.array[i]);
                }
                data.put("Win", winLoss[record.player]);
                data.put("Ordinal", ordinal[record.player]);
                data.put("Score", finalScores[record.player]);
                logger.record(data);
            }
            currentData = new ArrayList<>();
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        // we record one state for each player after every action is taken
        if (type == CoreConstants.GameEvents.ACTION_TAKEN) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                double[] phi = phiFn.featureVector(state, p);
                currentData.add(new DoubleArrayWrapper(p, phi));
            }
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    // To avoid incessant boxing / unboxing if we were to use Double
    static class DoubleArrayWrapper {
        final int player;
        final double[] array;

        DoubleArrayWrapper(int player, double[] contents) {
            array = contents;
            this.player = player;
        }
    }

}
