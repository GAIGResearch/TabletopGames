package core;

import components.*;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public abstract class Game {

    /**
     * List of agents/players that play this game.
     */
    protected List<AIPlayer> players;

    /**
     * Real game state
     */
    protected GameState gameState;

    /**
     * GameState observations as seen by different players.
     */
    protected GameState[] gameStateObservations;


    /**
     * Inits the game. Simply wires the references between game state, parameters and forward models
     * @param gp Game parameters
     * @param gs GameState subclass for this game.
     * @param fm ForwardModel subclass for this game.
     */
    void init(GameParameters gp, GameState gs, ForwardModel fm) {
        this.gameState = gs;
        this.gameState.setGameParameters(gp);
        this.gameState.init();
        this.gameState.setForwardModel(fm);
    }

    /**
     * Sets the game in the initial state.
     * @param dataPath path to the directory that has all data files.
     * @param players List of players for this game.
     */
    void setup(String dataPath, List<AIPlayer> players) {
        this.players = players;
        gameState.setNPlayers(players.size());
        gameState.setComponents(dataPath);
        gameState.getModel().setup(gameState);
    }

    public GameState getGameState() {  return gameState; }
    public List<AIPlayer> getPlayers() { return players; }

    /* List of functions to be implemented in a subclass */
    public abstract void run(GUI gui);
    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();


}
