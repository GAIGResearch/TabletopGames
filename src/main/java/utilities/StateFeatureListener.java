package utilities;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IStateFeatureVector;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished and we know the final result, then the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public class StateFeatureListener implements IGameListener {

    IStateFeatureVector phiFn;

    public StateFeatureListener(IStateFeatureVector phi) {
        this.phiFn = phi;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {

        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == CoreConstants.GameEvents.ACTION_TAKEN) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                double[] phi = phiFn.featureVector(state, p);
            }
        }
    }

    @Override
    public void allGamesFinished() {

    }
}
