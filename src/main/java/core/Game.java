package core;

import core.observations.IObservation;
import players.AbstractPlayer;

import java.util.HashSet;
import java.util.List;

public abstract class Game {

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

    // Real game state
    protected AbstractGameState gameState;
    public AbstractGameState getGameState(){return gameState;}
    protected ForwardModel forwardModel;

    // GameState core.observations as seen by different players.
    protected IObservation[] gameStateObservations;

    /* List of functions to be implemented in a subclass */
    public abstract void run(GUI gui);
    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();
}
