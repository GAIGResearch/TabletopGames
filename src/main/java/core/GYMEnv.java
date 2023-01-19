package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
import games.GameType;
import games.tictactoe.TicTacToeConstants;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.TicTacToeStateVector;
import players.human.HumanGUIPlayer;
import players.python.PythonAgent;
import utilities.Utils;


import java.util.Arrays;
import java.util.List;


public class GYMEnv {
    private Game game;
    private AbstractGameState gameState;
    private AbstractForwardModel forwardModel;
    private List<AbstractPlayer> players;
    private int turnPause = 0;
    private boolean debug = false;
    private int tick;
    private int lastPlayer; // used to track actions per 'turn'
    private List<AbstractAction> availableActions;


    // todo: set-up everything required to run an actual game in Pandemic
    //  - functions: init, getObs, step, finalise...
    public GYMEnv(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed) {
        //                              boolean randomizeParameters, List<IGameListener> listeners
        this.players = players;
        // Creating game instance (null if not implemented)
        if (parameterConfigFile != null) {
            AbstractParameters params = ParameterFactory.createFromFile(gameToPlay, parameterConfigFile);
            this.game = gameToPlay.createGameInstance(players.size(), seed, params);
        } else game = gameToPlay.createGameInstance(players.size(), seed);
//        if (game != null) {
//            if (listeners != null)
//                listeners.forEach(game::addListener);
//
//            // Randomize parameters
//            if (randomizeParameters) {
//                AbstractParameters gameParameters = game.getGameState().getGameParameters();
//                gameParameters.randomize();
//            }
//
//        }

    }

    public AbstractGameState reset(){
        // Reset game instance, passing the players for this game
        this.game.reset(players);
        this.turnPause = 0;
        this.tick = 0;
        this.game.setTurnPause(turnPause);
        this.gameState = game.getGameState();
        this.forwardModel = game.getForwardModel();
        this.availableActions = forwardModel.computeAvailableActions(gameState);
        return this.gameState;
    }

    public int getPlayerID(){
        return gameState.getCurrentPlayer();
    }

    public boolean isDone(){
        return !gameState.isNotTerminal();
    }

    public double getReward(){
        return gameState.getGameScore(gameState.getCurrentPlayer());
    }

    public List<AbstractAction> getActions(){
        return availableActions;
    }

    public double[] getFeatures(){
        String playerChar = TicTacToeConstants.playerMapping.get(gameState.getCurrentPlayer()).getTokenType();
        TicTacToeGameState gs = (TicTacToeGameState) gameState;
        return Arrays.stream(gs.getGridBoard().flattenGrid()).mapToDouble(c -> {
            String pos = ((Token) c).getTokenType();
            if (pos.equals(playerChar)) {
                return 1.0;
            } else if (pos.equals(TicTacToeConstants.emptyCell)) {
                return 0.0;
            } else { // opponent's piece
                return -1.0;
            }
        }).toArray();
//        return new TicTacToeStateVector().featureVector(gameState, gameState.getCurrentPlayer());
    }

    public AbstractGameState step(int a){
        // execute action and loop until a PythonAgent is required to make a decision
        AbstractAction a_ = this.availableActions.get(a);
        forwardModel.next(gameState, a_);

        int activePlayer = gameState.getCurrentPlayer();
        AbstractPlayer currentPlayer = players.get(activePlayer);
        while ( !(currentPlayer instanceof PythonAgent)){
            AbstractGameState observation = gameState.copy(activePlayer);
            List<core.actions.AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);

            if (isDone()){
                // game is over
                return observation;
            }

            // Start the timer for this decision
            gameState.playerTimer[activePlayer].resume();

            // Either ask player which action to use or, in case no actions are available, report the updated observation
            core.actions.AbstractAction action = null;
            if (observedActions.size() > 0) {
                if (observedActions.size() == 1 && (!(currentPlayer instanceof HumanGUIPlayer) || observedActions.get(0) instanceof DoNothing)) {
                    // Can only do 1 action, so do it.
                    action = observedActions.get(0);
                    currentPlayer.registerUpdatedObservation(observation);
                } else {
                    // Get action from player, and time it
                    action = currentPlayer.getAction(observation, observedActions);
                }
            } else {
                currentPlayer.registerUpdatedObservation(observation);
            }

            // End the timer for this decision
            gameState.playerTimer[activePlayer].pause();
            gameState.playerTimer[activePlayer].incrementAction();

            if (gameState.coreGameParameters.verbose && !(action == null)) {
                System.out.println(action);
            }
            if (action == null)
                throw new AssertionError("We have a NULL action in the Game loop");

            // Check player timeout
            forwardModel.next(gameState, action);
            tick++;

            lastPlayer = activePlayer;
            currentPlayer = players.get(gameState.getCurrentPlayer());

        }
        AbstractGameState observation = gameState.copy(activePlayer);
        this.availableActions = forwardModel.computeAvailableActions(observation);

        return observation;
    }

    public int getTick(){
        return this.tick;
    }

    public Utils.GameResult[] getPlayerResults(){
        return this.gameState.getPlayerResults();
    }

}