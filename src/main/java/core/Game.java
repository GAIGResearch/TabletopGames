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

            // either ask player which action to use or, in case no actions are available, report the updated observation
            int actionIdx = -1;
            if (actions.size() > 0) {
                if (player instanceof HumanGUIPlayer) {
                    while (actionIdx == -1) {
                        actionIdx = getPlayerAction(gui, player, observation, actions);
                    }
                } else {
                    actionIdx = getPlayerAction(gui, player, observation, actions);
                }
            } else {
                player.registerUpdatedObservation(observation);
            }

            // Resolve actions and game rules for the turn
            if (actionIdx != -1)
                forwardModel.next(gameState, actions.get(actionIdx));
        }

        gameState.endGame();
        System.out.println("Game Over");
    }

    // Public methods
    public final AbstractGameState getGameState(){return gameState;}

    private int getPlayerAction(GUI gui, AbstractPlayer player, IObservation observation, List<IAction> actions) {
        int actionIdx = player.getAction(observation, actions);

        if (gui != null) {
            gui.update(player, gameState);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println("EXCEPTION " + e);
            }
        }

        return actionIdx;
    }
}
