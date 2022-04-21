package utilities;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IGameListener;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStatisticLogger;

import java.util.*;
import java.util.stream.IntStream;

import static utilities.StateFeatureListener.*;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public class ActionFeatureListener implements IGameListener {

    IActionFeatureVector psiFn;
    List<LocalDataWrapper> currentData = new ArrayList<>();
    IStatisticLogger logger;

    public ActionFeatureListener(IStatisticLogger logger, IActionFeatureVector psi) {
        this.psiFn = psi;
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

            for (LocalDataWrapper record : currentData) {
                // we use a LinkedHashMap so that the order of the keys is preserved, and hence the
                // data is written to file in a sensible order for human viewing
                Map<String, Double> data = new LinkedHashMap<>();
                data.put("GameID", (double) game.getGameState().getGameID());
                data.put("Player", (double) record.player);
                data.put("Round", (double) record.gameRound);
                data.put("Turn", (double) record.gameTurn);
                data.put("CurrentScore", record.currentScore);
                for (int i = 0; i < record.array.length; i++) {
                    data.put(psiFn.names()[i], record.array[i]);
                }
                data.put("Win", winLoss[record.player]);
                data.put("Ordinal", ordinal[record.player]);
                data.put("FinalScore", finalScores[record.player]);
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
                double[] phi = psiFn.featureVector(action, state, p);
                currentData.add(new LocalDataWrapper(p, phi, state));
            }
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }


}
