package core;

import core.actions.AbstractAction;
import core.gamephase.DefaultGamePhase;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class ForwardModel {

    // Random generator for this game.
    protected Random rnd;

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     * @param firstState - initial state.
     */
    public final void _setup(AbstractGameState firstState) {
        abstractSetup(firstState);
        setup(firstState);
    }

    /**
     * Performs initialisation of variables in the abstract game state.
     * @param firstState - the initial game state.
     */
    private void abstractSetup(AbstractGameState firstState) {
        rnd = new Random(firstState.getGameParameters().getGameSeed());

        firstState.availableActions = new ArrayList<>();

        firstState.gameStatus = Utils.GameResult.GAME_ONGOING;
        firstState.playerResults = new Utils.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, Utils.GameResult.GAME_ONGOING);

        firstState.gamePhase = DefaultGamePhase.Main;
    }

    /**
     * Performs initial game setup according to game rules
     *  - sets up decks and shuffles
     *  - gives player cards
     *  - places tokens on boards
     *  etc.
     * @param firstState - the state to be modified to the initial game state.
     */
    protected abstract void setup(AbstractGameState firstState);

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    public abstract void next(AbstractGameState currentState, AbstractAction action);
}
