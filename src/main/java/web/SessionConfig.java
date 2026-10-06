package web;

import games.GameType;

/**
 * What a new {@link GameSession} plays: the game, the number of players, which seat the browser player takes, and the
 * agent in every other seat.
 *
 * @param opponent   a {@link players.PlayerFactory} key ("mcts", "random", ...) or an agent JSON file
 * @param seed       the game seed, or -1 for a new seed for each game
 * @param turnPause  ms the game waits after each action, so the browser player can see what the AIs did
 * @param showFrames place the Swing frames on screen (for debugging); otherwise they are off screen
 */
public record SessionConfig(String game, int nPlayers, int seat, String opponent, long seed, int turnPause,
                            boolean showFrames) {

    public GameType gameType() {
        return GameType.valueOf(game);
    }

    public void validate() {
        GameType gameType = gameType();
        if (nPlayers < gameType.getMinPlayers() || nPlayers > gameType.getMaxPlayers())
            throw new IllegalArgumentException(game + " takes " + gameType.getMinPlayers() + " to " + gameType.getMaxPlayers() + " players, not " + nPlayers);
        if (seat < 0 || seat >= nPlayers)
            throw new IllegalArgumentException("seat must be between 0 and " + (nPlayers - 1));
    }
}
