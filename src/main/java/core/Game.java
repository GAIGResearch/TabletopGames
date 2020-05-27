package core;

import core.actions.AbstractAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.HumanGUIPlayer;

import java.util.Collections;
import java.util.List;

import utilities.CoreConstants;

public abstract class Game {

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

    // Real game state and forward model
    protected AbstractGameState gameState;
    protected ForwardModel forwardModel;

    // GameState observations as seen by different players.
    protected IObservation[] gameStateObservations;

    /**
     * Game constructor. Receives a list of players, a forward model and a game state. Sets unique and final
     * IDs to all players in the game, and performs initialisation of the game state and forward model objects.
     * @param players - players taking part in this game.
     * @param model - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public Game(List<AbstractPlayer> players, ForwardModel model, AbstractGameState gameState) {
        this.forwardModel = model;
        this.gameState = gameState;
        this.forwardModel._setup(gameState);
        this.gameState.addAllComponents();

        this.players = players;
        int id = 0;
        for (AbstractPlayer player: players) {
            IObservation observation = this.gameState.getObservation(id);
            player.playerID = id++;
            player.initializePlayer(observation);
        }
    }

    /**
     * Runs the game, given a GUI. If this is null, the game runs automatically without visuals.
     * @param gui - graphical user interface.
     */
    public final void run(GUI gui) {

        while (gameState.isNotTerminal()){
            if (CoreConstants.VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            AbstractPlayer player = players.get(activePlayer);

            // Get actions for the player
            List<AbstractAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && CoreConstants.VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            // Either ask player which action to use or, in case no actions are available, report the updated observation
            int actionIdx = -1;
            if (actions.size() > 1) {
                if (player instanceof HumanGUIPlayer) {
                    while (actionIdx == -1) {
                        actionIdx = getPlayerAction(gui, player, observation, actions);
                    }
                } else {
                    actionIdx = getPlayerAction(gui, player, observation, actions);
                }
            } else {
                player.registerUpdatedObservation(observation);
                // Can only do 1 action, so do it.
                if (actions.size() == 1) actionIdx = 0;
            }

            // Resolve actions and game rules for the turn
            if (actionIdx != -1)
                forwardModel.next(gameState, actions.get(actionIdx));
        }

        // Perform any end of game computations as required by the game
        gameState.endGame();
        System.out.println("Game Over");

        // Terminate players
        for (AbstractPlayer player: players) {
            player.finalizePlayer(gameState.getObservation(player.getPlayerID()));
        }
    }

    /**
     * Queries the player for an action, after performing of GUI update (if running with visuals).
     * @param gui - graphical user interface.
     * @param player - player being asked for an action.
     * @param observation - observation the player receives from the current game state.
     * @param actions - list of actions available in the current game state.
     * @return - int, index of action chosen by player (from the list of actions).
     */
    private int getPlayerAction(GUI gui, AbstractPlayer player, IObservation observation, List<AbstractAction> actions) {
        if (gui != null) {
            gui.update(player, gameState);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("EXCEPTION " + e);
            }
        }

        return player.getAction(observation, actions);
    }

    /**
     * Retrieves the current game state.
     * @return - current game state.
     */
    public final AbstractGameState getGameState() {
        return gameState;
    }
}
