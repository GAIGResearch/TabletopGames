package core;

import java.util.HashSet;
import java.util.List;

public abstract class Game {
    protected List<AIPlayer> players;
    protected GameState gameState;
    protected ForwardModel forwardModel;

    public void setPlayers(List<AIPlayer> players) {
        this.players = players;
    }

    public List<AIPlayer> getPlayers() {
        return players;
    }

    public abstract void run();
    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();
}
