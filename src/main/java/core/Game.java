package core;

import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.AbstractPlayer;

import java.util.Collections;
import java.util.List;

import static games.pandemic.PandemicConstants.VERBOSE;

public abstract class Game {

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

    // Real game state and forward model
    protected AbstractGameState gameState;
    protected ForwardModel forwardModel;

    protected IObservation[] gameStateObservations;

    public Game(List<AbstractPlayer> players) {
        this.players = players;
    }

    public void run(GUI gui) {
        while (!gameState.isTerminal()){
            if (VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            AbstractPlayer player = players.get(activePlayer);
            // Get actions for the player
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            int action = -1;
            while (action == -1) {
                action = player.getAction(observation, actions);

                if (gui != null) {
                    gui.update(player, gameState);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        System.out.println("EXCEPTION " + e);
                    }
                }
            }

            // Resolve actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));
            gameState.getTurnOrder().endPlayerTurnStep(gameState);
        }

        gameState.endGame();

        System.out.println("Game Over");
    }

    // Public methods
    public final AbstractGameState getGameState(){return gameState;}
}
