package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
import core.interfaces.IOrderedActionSpace;
import core.interfaces.IVectorisable;
import games.GameType;
import games.diamant.DiamantGameState;
import games.explodingkittens.ExplodingKittensGameState;
import games.tictactoe.TicTacToeConstants;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.TicTacToeStateVector;
import org.json.simple.JSONObject;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSPlayer;
import players.python.PythonAgent;
import players.simple.RandomPlayer;
import utilities.ActionTreeNode;
import utilities.Utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;


public class GYMEnv {
    private Game game;
    private AbstractGameState gameState;
    private IVectorisable iface;
    private AbstractForwardModel forwardModel;
    private List<AbstractPlayer> players;
    private int turnPause = 0;
    private boolean debug = false;
    private int tick;
    private int lastPlayer; // used to track actions per 'turn'
    private List<AbstractAction> availableActions;

    boolean isNormalized; // Bool for whether you want obersvations to be normalized

    private Random seedRandom; // Random used for setting the seed for each episode
    private long lastSeed;


    public GYMEnv(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed, boolean isNormalized) throws Exception {

        //                              boolean randomizeParameters, List<IGameListener> listeners
        this.seedRandom = new Random(seed);
        this.isNormalized = isNormalized;
        this.players = players;
        // Creating game instance (null if not implemented)
        if (parameterConfigFile != null) {
            AbstractParameters params = AbstractParameters.createFromFile(gameToPlay, parameterConfigFile);
            this.game = gameToPlay.createGameInstance(players.size(), seed, params);
        } else game = gameToPlay.createGameInstance(players.size(), seed);

        assert game != null;
        if (!(game.gameState instanceof IVectorisable && game.forwardModel instanceof IOrderedActionSpace)) {
            throw new Exception("Game has not implemented Reinforcement Learning Interface");
        }
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

    // --Wrappers for interface functions--

    // Gets observations in JSON
    public String getObservationJson() throws Exception {
        if (gameState instanceof IVectorisable) {
            return ((IVectorisable) gameState).getObservationJson();
        }
        else throw new Exception("Function is not implemented");
    }

    // Gets the observation space as an integer
    public int getObservationSpace() throws Exception {
        if (gameState instanceof IVectorisable) {
            return ((IVectorisable) gameState).getObservationSpace();
        }
        else throw new Exception("Function is not implemented");
    }

    public double[] getObservationVector() throws Exception {
        AbstractGameState gs = gameState.copy(gameState.getCurrentPlayer());
        if (gs instanceof IVectorisable) {
            if (isNormalized) return ((IVectorisable) gs).getNormalizedObservationVector();
            else return ((IVectorisable) gs).getObservationVector();
        }
        else throw new Exception("Function is not implemented");
    }

    // Gets the action space as an integer
    public int getActionSpace() throws Exception {
        if (forwardModel instanceof IOrderedActionSpace) {
            return ((IOrderedActionSpace) forwardModel).getActionSpace();
        }
        else throw new Exception("Function is not implemented");
    }

    // Gets the actions as an integer array
    public int[] getFixedActionSpace() throws Exception {
        if (forwardModel instanceof IOrderedActionSpace) {
            return ((IOrderedActionSpace) forwardModel).getFixedActionSpace();
        }
        else throw new Exception("Function is not implemented");
    }

    // Gets the action mask as a boolean array
    public int[] getActionMask() throws Exception {
        if (forwardModel instanceof IOrderedActionSpace) {
            return ((IOrderedActionSpace) forwardModel).getActionMask(gameState);
        }
        else throw new Exception("Function is not implemented");
    }

    public String getActionMaskJson() throws Exception {
        if (forwardModel.root != null) {
            return forwardModel.root.toJsonString();
        }
        else throw new Exception("Game does not implement action trees");
    }

    public List<ActionTreeNode> getFlattenedTree() throws Exception {
        if (forwardModel.root != null) {
            return forwardModel.root.flattenTree();
        }
        else throw new Exception("Game does not implement action trees");
    }

    // Plays an action given an actionID
    public void playAction(int actionID) throws Exception {
//        forwardModel.next(gameState, this.availableActions.get(actionID));
        if (forwardModel instanceof IOrderedActionSpace) {
            ((IOrderedActionSpace) forwardModel).nextPython(gameState, actionID);
        }
        else throw new Exception("Function is not implemented");
    }

    // --End of Wrapper Functions--


    public void reset(){
        // Reset game instance, run built-in agents until a python agent is required to make a decision
        this.game.reset(players);
        this.turnPause = 0;
        this.tick = 0;
        this.game.setTurnPause(turnPause);
        this.gameState = game.getGameState();
        this.lastSeed = seedRandom.nextLong();
        gameState.gameParameters.setRandomSeed(this.lastSeed);
        this.forwardModel = game.getForwardModel();
        this.availableActions = forwardModel.computeAvailableActions(gameState);

        // execute the game if needed until Python agent is required to make a decision
        int activePlayer = gameState.getCurrentPlayer();
        AbstractPlayer currentPlayer = players.get(activePlayer);
        while ( !(currentPlayer instanceof PythonAgent)){
            AbstractGameState observation = gameState.copy(activePlayer);
            List<core.actions.AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);

            if (isDone()){
                // game is over
                return;
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
            activePlayer = gameState.getCurrentPlayer();
            currentPlayer = players.get(gameState.getCurrentPlayer());
        }
        AbstractGameState observation = gameState.copy(activePlayer);
        this.availableActions = forwardModel.computeAvailableActions(observation);
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


    public AbstractGameState step(int a) throws Exception{
        // execute action and loop until a PythonAgent is required to make a decision
        if (isDone()){
            throw new Exception("Need to reset the environment after each finished episode");
        } else if (this.gameState == null){
            throw new Exception("Need to reset the environment before calling step");
        }
//        AbstractAction a_ = this.availableActions.get(a);
//        forwardModel.next(gameState, a_);
        playAction(a);
        if (isDone()){
            // check if the game has just ended
            // game is over
            return gameState.copy(gameState.getCurrentPlayer());
        }

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
            activePlayer = gameState.getCurrentPlayer();
            currentPlayer = players.get(gameState.getCurrentPlayer());

        }
        AbstractGameState observation = gameState.copy(activePlayer);
        this.availableActions = forwardModel.computeAvailableActions(observation);

        return observation;
    }

    public int getTick(){
        return this.tick;
    }

    public long getSeed(){
        return this.lastSeed;
    }

    public List getTreeShape(){
        return this.forwardModel.root.getTreeShape();
    }

    public CoreConstants.GameResult[] getPlayerResults(){
        return this.gameState.getPlayerResults();
    }

    public static void main(String[] args) {
        Long seed = new Long(2466);
        Random rnd = new Random(seed);
        ArrayList<AbstractPlayer> players = new ArrayList<>();
//        players.add(new MCTSPlayer());
        players.add(new PythonAgent());
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));

        try {
            GYMEnv env = new GYMEnv(GameType.valueOf("LoveLetter"), null, players, 343, true);
            boolean done = false;
            int episodes = 0;
            int MAX_EPISODES = 100;
            int steps = 0;
            env.reset();
            List<Object> treeShape = env.forwardModel.root.getTreeShape();
            int N_ACTIONS  = env.getActionSpace();
            while (!done){
//                int randomAction = rnd.nextInt(env.availableActions.size());
                // todo we get the action mask, but how do we know how we should index it?
                int[] mask = env.getActionMask();
//                mask[0] = 0; mask[1] = 0; mask[2] = 0;
                int[] trueIdx = IntStream.range(0, mask.length) // todo TTT valid actions (leaf nodes) start from 3
                        .filter(i -> mask[i] == 1)
                        .toArray();
                int randomAction = trueIdx[rnd.nextInt(trueIdx.length)];
//                int randomAction = rnd.nextInt(N_ACTIONS);
                try{
//                    System.out.println("playerID = " + env.getPlayerID());
                        double[] obs = env.getObservationVector();
//                    }

                    env.step(randomAction);
                } catch (Exception e){
                    System.out.println("Exception in GYMEnv main " + e.toString());
                }
                steps += 1;
                done = env.isDone();
                if (done){
                    episodes += 1;
                    System.out.println("episodes " + episodes + " is done in " + steps + " ; outcome:  " + env.getPlayerResults()[0].value);
                    if (episodes == MAX_EPISODES)break;
                    env.reset();
                    done = false;
                    steps = 0;

                }

            }
        } catch (Exception e){
            System.out.println("Exception in GYMEnv init" + e);
        }


    }

}
