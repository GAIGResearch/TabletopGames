package core;

import actions.Action;
import components.Counter;
import components.Deck;
import content.PropertyString;

import java.util.HashMap;
import java.util.List;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class GameState {
    protected Game game;
    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    protected int nPlayers;
    protected HashMap<Integer, Area> areas;
    public int roundStep;

    public abstract GameState copy();
    public GameState copyTo(GameState gs) {
        gs.activePlayer = activePlayer;

        // TODO: copy super game state objects
        return gs;
    }

    public int getActivePlayer() {
        return activePlayer;
    }

    public HashMap<Integer, Area> getAreas() {
        return areas;
    }

    public int nPlayers() {
        return nPlayers;
    }
    public abstract int nInputActions();  // How many actions are required by the game per player.
    public abstract int nPossibleActions();
    public abstract List<Action> possibleActions();


    public final void init(Game game)
    {
        this.game = game;
        areas = new HashMap<>();  // Game State has areas! Initialize.
    }

    public abstract void setup(Game game);
    public void setupAreas() {}

    public Deck findDeck(String name) {
        return game.findDeck(name);
    }
    public Counter findCounter(String name) {
        return game.findCounter(name);
    }

    public String tempDeck() {return game.tempDeck();}
    public void clearTempDeck() { game.clearTempDeck(); }


}
