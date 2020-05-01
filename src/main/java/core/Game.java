package core;

import games.explodingkittens.ExplodingKittensForwardModel;
import observations.Observation;
import players.AbstractPlayer;
import turnorder.TurnOrder;

import java.util.HashSet;
import java.util.List;

public abstract class Game {

    /**
     * List of agents/players that play this game.
     */
    protected List<AbstractPlayer> players;

    protected TurnOrder turnOrder;
    public TurnOrder getTurnOrder(){return turnOrder;}

    protected ForwardModel forwardModel;
    public ForwardModel getForwardModel(){return forwardModel;}

    /**
     * Real game state
     */
    protected AbstractGameState gameState;
    public AbstractGameState getGameState(){return gameState;}

    /**
     * GameState observations as seen by different players.
     */
    protected Observation[] gameStateObservations;


    /* List of functions to be implemented in a subclass */
    public abstract void run(GUI gui);
    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();
}
