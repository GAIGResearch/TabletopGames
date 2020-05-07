package core;

import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.HumanGUIPlayer;

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
        this.gameState.setComponents();
        this.forwardModel.setup(gameState);
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

            if (gui != null && player instanceof HumanGUIPlayer) {
                while (action == -1) {
                    action = player.getAction(observation, actions);
                    gui.update(player, gameState);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        System.out.println("EXCEPTION " + e);
                    }
                }
            } else {
                action = player.getAction(observation, actions);
            }
            System.out.println(actions.get(action).toString());

            // Resolve actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));
        }

        gameState.endGame();

        System.out.println("Game Over");
    }

    // Public methods
    public final AbstractGameState getGameState(){return gameState;}
}
