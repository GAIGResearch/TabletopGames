package core;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class GameState {
    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game

    public abstract GameState copy();
    public GameState copyTo(GameState gs) {
        gs.activePlayer = activePlayer;

        // TODO: copy super game state objects
        return gs;
    }

    public int getActivePlayer() {
        return activePlayer;
    }
}
