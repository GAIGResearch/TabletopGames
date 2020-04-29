package updated_core;

import updated_core.gamestates.AbstractGameState;
import updated_core.players.AbstractPlayer;

import java.util.HashSet;
import java.util.List;

public abstract class Game {

    /**
     * List of agents/players that play this game.
     */
    protected List<AbstractPlayer> players;

    /**
     * Real game state
     */
    protected AbstractGameState gameState;

    /**
     * Inits the game. Simply wires the references between game state, parameters and forward models
     * @param gp Game parameters
     * @param fm ForwardModel subclass for this game.
     */
    void init(GameParameters gp, ForwardModel fm) {
    }

    /**
     * Sets the game in the initial state.
     * @param dataPath path to the directory that has all data files.
     * @param players List of players for this game.
     */
    void setup(String dataPath, List<AbstractPlayer> players) {
    }

    public AbstractGameState getGameState() {  return gameState; }
    public List<AbstractPlayer> getPlayers() { return players; }

    /* List of functions to be implemented in a subclass */
    public abstract void run();

    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();


}
