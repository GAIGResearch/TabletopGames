package core;

import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;

import java.util.Collections;
import java.util.List;

import static utilities.CoreConstants.VERBOSE;

public abstract class Game {

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

    // Real game state and forward model
    protected AbstractGameState gameState;
    protected ForwardModel forwardModel;

    // GameState observations as seen by different players.
    protected IObservation[] gameStateObservations;

    public Game(List<AbstractPlayer> players, ForwardModel model, AbstractGameState gameState) {
        this.players = players;
        int id = 0;
        for (AbstractPlayer player: players) {
            player.playerID = id++;
        }
        this.forwardModel = model;
        this.gameState = gameState;
    }

    public void run(GUI gui) {
        gameState.setComponents();
        forwardModel.setup(gameState);

        while (!gameState.isTerminal()){
            if (VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            // Get actions for the player
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            int action = players.get(activePlayer).getAction(observation, actions);

            // Resolve core.actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));

            if (gui != null) {
                gui.update(gameState);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }
        }

        gameState.endGame();

        System.out.println("Game Over");
    }

    // Public methods
    public final AbstractGameState getGameState(){return gameState;}
}
