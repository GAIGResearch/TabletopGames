package core.interfaces;

import core.Game;

public interface IGameHeuristic {


    /**
     * Returns a score for the game that should be maximised. It is anticipated that this will be
     * used after a game has completed (but that is not mandatory).
     * We use the Game instead of the AbstractGameState (see IStateHeuristic) because we may want
     * access to the Players in a Game, for example if we are tuning a game to have a Random player
     * do well (OK, that's a rather forced example, but you get the drift).
     * @param game - game state to evaluate and score.
     * @return - value of given state.
     */
    double evaluateGame(Game game);


}
