package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.GameType;
import players.human.HumanGUIPlayer;
import players.python.PythonAgent;
import utilities.Utils;


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

    public void reset(){
        // Reset game instance, passing the players for this game
        this.game.reset(players);
        this.turnPause = 0;
        this.tick = 0;
        this.game.setTurnPause(turnPause);
        this.gameState = game.getGameState();
        this.forwardModel = game.getForwardModel();
        this.availableActions = forwardModel.computeAvailableActions(gameState);
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

    public AbstractGameState step(int a){
        // execute action and loop until a PythonAgent is required to make a decision
        // todo check for game over
        // todo need the vectorised obs here
        // todo instead of returning a GS we should wrap it into obs, reward, done, info
        AbstractAction a_ = this.availableActions.get(a);
        forwardModel.next(gameState, a_);

        int activePlayer = gameState.getCurrentPlayer();
        AbstractPlayer currentPlayer = players.get(activePlayer);
        while ( !(currentPlayer instanceof PythonAgent)){
            AbstractGameState observation = gameState.copy(activePlayer);
            List<core.actions.AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);

            if (isDone()){
                //TODO game is over
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
//                    System.out.println("Opponent choose action");
                }
//                if (gameState.coreGameParameters.competitionMode && action != null && !observedActions.contains(action)) {
//                    action = null;
//                }
//                // We publish an ACTION_CHOSEN message before we implement the action, so that observers can record the state that led to the decision
//                core.actions.AbstractAction finalAction = action;
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
//            if (observation.playerTimer[activePlayer].exceededMaxTime()) {
//                forwardModel.disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnTimeout, gameState);
//            } else {
//                // Resolve action and game rules, time it
//                forwardModel.next(gameState, action);
//            }
            tick++;

            lastPlayer = activePlayer;

        }
        AbstractGameState observation = gameState.copy(activePlayer);
        this.availableActions = forwardModel.computeAvailableActions(observation);
//        if (currentPlayer instanceof PythonAgent){
//            return observation;
//        }
        return observation;

//        // todo if agent is dummyAgent return obs, otherwise ask JAVA agents for actions
//        if (debug) System.out.printf("Starting oneAction for player %s%n", activePlayer);
//
//        // Get player observation, and time how long it takes
//        double s = System.nanoTime();
//        // copying the gamestate also copies the game parameters and resets the random seed (so agents cannot use this
//        // to reconstruct the starting hands etc.)
//        AbstractGameState observation = gameState.copy(activePlayer);
//        if (currentPlayer instanceof PythonAgent){
//            return observation;
//        }
////        copyTime += (System.nanoTime() - s);
//
//        // Get actions for the player
//        s = System.nanoTime();
//        List<core.actions.AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);
////        actionComputeTime += (System.nanoTime() - s);
////        actionSpaceSize.add(new Pair<>(activePlayer, observedActions.size()));
//
//        if (gameState.coreGameParameters.verbose) {
//            System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());
//        }
//
//        if (observation instanceof IPrintable && gameState.coreGameParameters.verbose) {
//            ((IPrintable) observation).printToConsole();
//        }
//
//        // Start the timer for this decision
//        gameState.playerTimer[activePlayer].resume();
//
//        // Either ask player which action to use or, in case no actions are available, report the updated observation
//        core.actions.AbstractAction action = null;
//        if (observedActions.size() > 0) {
//            if (observedActions.size() == 1 && (!(currentPlayer instanceof HumanGUIPlayer) || observedActions.get(0) instanceof DoNothing)) {
//                // Can only do 1 action, so do it.
//                action = observedActions.get(0);
//                currentPlayer.registerUpdatedObservation(observation);
//            } else {
//                // Get action from player, and time it
//                s = System.nanoTime();
//                if (debug) System.out.printf("About to get action for player %d%n", gameState.getCurrentPlayer());
//                action = currentPlayer.getAction(observation, observedActions);
////                agentTime += (System.nanoTime() - s);
////                nDecisions++;
//            }
//            if (gameState.coreGameParameters.competitionMode && action != null && !observedActions.contains(action)) {
//                System.out.printf("Action played that was not in the list of available actions: %s%n", action.getString(gameState));
//                action = null;
//            }
//            // We publish an ACTION_CHOSEN message before we implement the action, so that observers can record the state that led to the decision
//            core.actions.AbstractAction finalAction = action;
////            listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ACTION_CHOSEN, gameState, finalAction));
//        } else {
//            currentPlayer.registerUpdatedObservation(observation);
//        }
//
//        // End the timer for this decision
//        gameState.playerTimer[activePlayer].pause();
//        gameState.playerTimer[activePlayer].incrementAction();
//
//        if (gameState.coreGameParameters.verbose && !(action == null)) {
//            System.out.println(action);
//        }
//        if (action == null)
//            throw new AssertionError("We have a NULL action in the Game loop");
//
//        // Check player timeout
//        if (observation.playerTimer[activePlayer].exceededMaxTime()) {
//            forwardModel.disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnTimeout, gameState);
//        } else {
//            // Resolve action and game rules, time it
//            s = System.nanoTime();
//            forwardModel.next(gameState, action);
////            nextTime += (System.nanoTime() - s);
//        }
//        tick++;
//
//        lastPlayer = activePlayer;
//
//        // We publish an ACTION_TAKEN message once the action is taken so that observers can record the result of the action
//        // (such as the next player)
//        AbstractAction finalAction1 = action;
////        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ACTION_TAKEN, gameState.copy(), finalAction1.copy()));
//        if (debug) System.out.printf("Finishing oneAction for player %s%n", activePlayer);
//        return observation;
    }

    public int getTick(){
        return this.tick;
    }

}
