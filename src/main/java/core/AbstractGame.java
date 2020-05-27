package core;

import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import players.HumanGUIPlayer;

import java.util.List;

public abstract class AbstractGame {

    // List of agents/players that play this game.
    protected List<AbstractPlayer> players;

    // Real game state and forward model
    protected AbstractGameState gameState;
    protected AbstractForwardModel forwardModel;

    /**
     * Game constructor. Receives a list of players, a forward model and a game state. Sets unique and final
     * IDs to all players in the game, and performs initialisation of the game state and forward model objects.
     * @param players - players taking part in this game.
     * @param playerFMs - forward models used to apply game rules, one for each player (with different random seeds).
     * @param realModel - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public AbstractGame(List<AbstractPlayer> players, List<AbstractForwardModel> playerFMs, AbstractForwardModel realModel, AbstractGameState gameState) {
        this.gameState = gameState;
        this.forwardModel = realModel;
        this.forwardModel._setup(gameState);
        this.gameState.addAllComponents();

        this.players = players;
        int id = 0;
        for (AbstractPlayer player: players) {
            // Retrieve the FM for this player
            player.forwardModel = playerFMs.get(id);
            // Create initial state observation
            AbstractGameState observation = gameState._copy(id);
            // Give player their ID
            player.playerID = id++;
            // Allow player to initialize
        }
    }

    /**
     * Runs the game, given a GUI. If this is null, the game runs automatically without visuals.
     * @param gui - graphical user interface.
     */
    public final void run(AbstractGUI gui) {

        while (gameState.isNotTerminal()){
            if (CoreConstants.VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            AbstractPlayer player = players.get(activePlayer);

            // Get actions for the player
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractGameState observation = gameState._copy(activePlayer);
            if (observation != null && CoreConstants.VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            // Either ask player which action to use or, in case no actions are available, report the updated observation
            int actionIdx = -1;
            if (actions.size() > 1) {
                if (player instanceof HumanGUIPlayer) {
                    while (actionIdx == -1) {
                        actionIdx = getPlayerAction(gui, player, observation);
                    }
                } else {
                    actionIdx = getPlayerAction(gui, player, observation);
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
        forwardModel.endGame(gameState);
        System.out.println("Game Over");

        // Allow players to terminate
        for (AbstractPlayer player: players) {
            player.finalizePlayer(gameState._copy(player.getPlayerID()));
        }
    }

    /**
     * Queries the player for an action, after performing of GUI update (if running with visuals).
     * @param gui - graphical user interface.
     * @param player - player being asked for an action.
     * @param observation - observation the player receives from the current game state.
     * @return - int, index of action chosen by player (from the list of actions).
     */
    private int getPlayerAction(AbstractGUI gui, AbstractPlayer player, AbstractGameState observation) {
        if (gui != null) {
            gui.update(player, gameState);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("EXCEPTION " + e);
            }
        }

        return player.getAction(observation);
    }

    /**
     * Retrieves the current game state.
     * @return - current game state.
     */
    public final AbstractGameState getGameState() {
        return gameState;
    }
}
