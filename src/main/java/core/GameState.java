package core;

import components.Counter;
import components.Deck;
import content.PropertyString;

import java.util.HashMap;
import java.util.List;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class GameState {
    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    protected int nPlayers;  // TODO: initialize this
    protected HashMap<Integer, Area> areas;

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

    public Deck findDeck(int deckFrom) {
        // TODO: find deck by index?
        return null;
    }

    public Counter findCounter(int infectionCounterHash) {
        // TODO: do it
        return null;
    }

    public int nPlayers() {
        return nPlayers;
    }
}
