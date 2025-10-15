package core;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.interfaces.IStateFeatureVector;
import core.interfaces.ITreeActionSpace;
import core.interfaces.IStateFeatureJSON;
import games.GameType;
import games.diamant.DiamantFeatures;
import games.loveletter.features.LLStateFeaturesReduced;
import games.powergrid.PowerGridFeatures;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters;
import games.powergrid.components.PowerGridCard;
import games.stratego.StrategoFeatures;
import games.sushigo.SGFeatures;
import games.tictactoe.TTTFeatures;
import org.json.simple.JSONObject;
import players.human.HumanGUIPlayer;
import players.python.PythonAgent;
import players.simple.RandomPlayer;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

enum FeatureExtractors {
    /* Every game implementing the RL interfaces should be registered here, PyTAG uses this to reference the correct features extractors and action spaces
    * In case that an interface is not implemented they can be set to null*/
  //  ExplodingKittens( ExplodingKittensFeatures.class, null),
    LoveLetter(LLStateFeaturesReduced.class, null),
    Stratego(StrategoFeatures.class, null),
    SushiGo(null, SGFeatures.class),
    TicTacToe(TTTFeatures.class, TTTFeatures.class),
    Diamant(DiamantFeatures.class, DiamantFeatures.class),
	PowerGrid(PowerGridFeatures.class, null); //gets both the JSON and Vector observation 
    Class<? extends IStateFeatureVector> stateFeatureVector;
    Class<? extends IStateFeatureJSON> stateFeatureJSON;
    FeatureExtractors(Class<? extends IStateFeatureVector> stateFeatureVector, Class<? extends IStateFeatureJSON> stateFeatureJSON) {
        this.stateFeatureVector = stateFeatureVector;
        this.stateFeatureJSON = stateFeatureJSON;
    }

    @Override
    public String toString() {
        return "name:" + this.name() + " vector:" + (this.stateFeatureVector != null) + " json:" + (this.stateFeatureJSON != null);
    }

    public IStateFeatureVector getStateFeatureVector() {
        if (stateFeatureVector == null) return null;
        try {
            return stateFeatureVector.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public IStateFeatureJSON getStateFeatureJSON() {
        if (stateFeatureJSON == null) return null;
        try {
            return stateFeatureJSON.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}

public class PyTAG {
    private Game game;
    // root of the action tree
    private ActionTreeNode root;
    // list of leaf nodes
    private List<ActionTreeNode> leaves;
    private AbstractGameState gameState;
    private AbstractForwardModel forwardModel;
    private IStateFeatureVector stateVectoriser;
    private IStateFeatureJSON stateJSONiser;
    private List<AbstractPlayer> players;
    private int turnPause = 0;
    private int tick;
    private int lastPlayer; // used to track actions per 'turn'
    private List<AbstractAction> availableActions;

    boolean isNormalized; // Bool for whether you want observations to be normalized

    private Random seedRandom; // Random used for setting the seed for each episode
    private long lastSeed;

    public static String getSupportedGames(){
        /* returns the supported games with the corresponding feature extractors */
        String supportedGames = "";
        for (FeatureExtractors fe : FeatureExtractors.values()) {
            supportedGames += fe.toString() + "\n";
//            System.out.println(fe);
        }
        return supportedGames;
    }

    public static String getSupportedGamesJSON(){
        /* returns the supported games with the corresponding feature extractors */
        JSONObject json = new JSONObject();
        String supportedGames = "";
        for (FeatureExtractors fe : FeatureExtractors.values()) {
            JSONObject features = new JSONObject();
            features.put("vector", fe.stateFeatureVector != null);
            features.put("json", fe.stateFeatureJSON != null);
            json.put(fe.name(), features);
        }
        return json.toJSONString();
    }


    public PyTAG(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed, boolean isNormalized) throws Exception {

        // boolean randomizeParameters, List<IGameListener> listeners
        this.seedRandom = new Random(seed);
        this.isNormalized = isNormalized;
        this.players = players;
        this.stateVectoriser = FeatureExtractors.valueOf(gameToPlay.name()).getStateFeatureVector();
        this.stateJSONiser = FeatureExtractors.valueOf(gameToPlay.name()).getStateFeatureJSON();

        // Creating game instance (null if not implemented)
        if (parameterConfigFile != null) {
            AbstractParameters params = AbstractParameters.createFromFile(gameToPlay, parameterConfigFile);
            this.game = gameToPlay.createGameInstance(players.size(), seed, params);
        } else game = gameToPlay.createGameInstance(players.size(), seed);

        assert game != null;

        if (this.stateVectoriser == null && this.stateJSONiser == null){
            throw new Exception("Game does not implement the state feature vector or JSON interface");
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
        AbstractGameState gs = gameState.copy(gameState.getCurrentPlayer());
        if (stateJSONiser != null){
            return stateJSONiser.getObservationJson(gs, gs.getCurrentPlayer());
        }
        else throw new Exception("JSON feature extractor is not implemented");
    }

    // Gets the observation space as an integer
    public int getObservationSpace() {
        if (stateVectoriser != null){
            return stateVectoriser.names().length;
        }
        return 0; // dummy value
//        else throw new Exception("Function is not implemented");

    }

    public double[] getObservationVector() throws Exception {
        AbstractGameState gs = gameState.copy(gameState.getCurrentPlayer());
        if (stateVectoriser != null){
            return stateVectoriser.doubleVector(gs, gs.getCurrentPlayer());
        }
        else throw new Exception("Observation vectoriser function is not implemented");
    }

    // Gets the action space size as an integer
    public int getActionSpace(){
        return leaves.size();
    }

    // Gets the actions as an integer array
    public int[] getFixedActionSpace() {
        return new int[this.leaves.size()];

    }

    // Gets the action mask as a boolean array
    public int[] getActionMask() {
        return leaves.stream()
                .mapToInt(ActionTreeNode::getValue)
                .toArray();
    }

    // gets the whole action tree as an array (tree can be reconstructed using the getTreeShape() function)
    public int[] getActionTree() {
        return root.getActionMask();
    }

    // gets the action tree shape as a list of arrays
    public List getTreeShape(){
        return this.root.getTreeShape();
    }

    // Plays an action given an actionID
    public void executeAction(int actionID) throws Exception {
        if (forwardModel instanceof ITreeActionSpace) {
            ActionTreeNode node = leaves.get(actionID);
            AbstractAction action = node.getAction();
            forwardModel.next(gameState, action);
        }
        else throw new Exception("Function is not implemented");
    }

    // --End of Wrapper Functions--
    /*
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
        boolean isTerminal = nextDecision();

        // get action tree for current player
        if (this.root == null){
            this.root = ((ITreeActionSpace)this.forwardModel).initActionTree(this.gameState);
        }
        // update with initial actions
        // Compute the updated available actions and the action tree
        AbstractGameState observation = gameState.copy(gameState.getCurrentPlayer());
        this.availableActions = forwardModel.computeAvailableActions(observation);
        this.root = ((ITreeActionSpace)this.forwardModel).updateActionTree(this.root, this.gameState);
        this.leaves = root.getLeafNodes();
    }
*/
    public void reset() {
        this.game.reset(players);
        this.turnPause = 0;
        this.tick = 0;
        this.game.setTurnPause(turnPause);
        this.gameState = game.getGameState();
        this.lastSeed = seedRandom.nextLong();
        gameState.gameParameters.setRandomSeed(this.lastSeed);
        this.forwardModel = game.getForwardModel();

        // probe the very first state we see
        PGProbe.Snap s0 = PGProbe.snap((PowerGridGameState) gameState);

        // ⚠️ computeAvailableActions SHOULD be pure. Verify it:
        PGProbe.Snap s1a = PGProbe.snap((PowerGridGameState) gameState);
        this.availableActions = forwardModel.computeAvailableActions(gameState);
        PGProbe.Snap s1b = PGProbe.snap((PowerGridGameState) gameState);
        PGProbe.diff("after computeAvailableActions(gameState)", s1a, s1b);

        // This typically runs until the Python player must act (and CAN mutate a lot)
        PGProbe.Snap s2a = PGProbe.snap((PowerGridGameState) gameState);
        PGProbe.Snap s2b = PGProbe.snap((PowerGridGameState) gameState);
        PGProbe.diff("after nextDecision()", s2a, s2b);

        if (this.root == null){
            this.root = ((ITreeActionSpace)this.forwardModel).initActionTree(this.gameState);
        }

        // Use an observation copy for available actions (should be pure)
        AbstractGameState observation = gameState.copy(gameState.getCurrentPlayer());
        PGProbe.Snap s3a = PGProbe.snap((PowerGridGameState) gameState);
        this.availableActions = forwardModel.computeAvailableActions(observation);
        PGProbe.Snap s3b = PGProbe.snap((PowerGridGameState) gameState);
        PGProbe.diff("after computeAvailableActions(observation)", s3a, s3b);

        // Keep the same state you used for actions when updating tree to avoid mismatches
        PGProbe.Snap s4a = PGProbe.snap((PowerGridGameState) gameState);
        this.root = ((ITreeActionSpace)this.forwardModel).updateActionTree(this.root, observation);
        PGProbe.Snap s4b = PGProbe.snap((PowerGridGameState) gameState);
        PGProbe.diff("after updateActionTree(root, observation)", s4a, s4b);

        this.leaves = root.getLeafNodes();

        // Final summary vs very first
        PGProbe.diff("SUMMARY reset(): start→end", s0, PGProbe.snap((PowerGridGameState) gameState));
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

    // Executes game loop until RL agent is required to make a decision
    // returns true if game is over
    public boolean nextDecision(){
        int activePlayer = gameState.getCurrentPlayer();
        AbstractPlayer currentPlayer = players.get(activePlayer);
        while ( !(currentPlayer instanceof PythonAgent)){
            AbstractGameState observation = gameState.copy(activePlayer);
            List<core.actions.AbstractAction> observedActions = forwardModel.computeAvailableActions(observation);

            if (isDone()){
                // game is over
                return true;
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
        return false;
    }


    public AbstractGameState step(int actionId) throws Exception{
        // execute action and loop until an RL agent is required to make a decision
        if (isDone()){
            throw new Exception("Need to reset the environment after each finished episode");
        } else if (this.gameState == null){
            throw new Exception("Need to reset the environment before calling step");
        }
        // executes the seleted actions
        executeAction(actionId);
        if (isDone()){
            // check if the game has just ended
            // game is over
            return gameState.copy(gameState.getCurrentPlayer());
        }

        // update game until RL agent is required to make a decision - if game is over in the mean time returns isTerminal
        boolean isTerminal = nextDecision();
        if (isTerminal){
            // game is over
            return gameState.copy(gameState.getCurrentPlayer());
        }

        int activePlayer = gameState.getCurrentPlayer();
        AbstractGameState observation = gameState.copy(activePlayer);

        // Compute the updated available actions and the action tree
        this.availableActions = forwardModel.computeAvailableActions(observation);
        this.root = ((ITreeActionSpace)this.forwardModel).updateActionTree(this.root, this.gameState);
        this.leaves = root.getLeafNodes();

        return observation;
    }

    public int getTick(){
        return this.tick;
    }

    public long getSeed(){
        return this.lastSeed;
    }

    public CoreConstants.GameResult[] getPlayerResults(){
        return this.gameState.getPlayerResults();
    }

    public int sampleRNDAction(int[] mask, Random rnd){
        /* take the action mask and sample a valid random action */
        int[] trueIdx = IntStream.range(0, mask.length)
                .filter(i -> mask[i] == 1)
                .toArray();
        int rndIdx = trueIdx[rnd.nextInt(trueIdx.length)];
        return rndIdx;
    }

    public static void main(String[] args) {
        long seed = 3;
        Random rnd = new Random(seed);
        int numberOfSteps = 10;

        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new PythonAgent());     // slot 0: the “learning” seat
        players.add(new RandomPlayer(rnd)); // other seats: random
        players.add(new RandomPlayer(rnd));

        try {
            // PowerGrid (not TicTacToe)
            PyTAG env = new PyTAG(GameType.PowerGrid, null, players, seed, false);

            // Reset to the first Python turn
            env.reset();

            // Fixed “head” order for sanity checks
            List<String> head = env.getLeafNames();
            System.out.println("HEAD size: " + head.size());

            // --- Initial snapshot ---
            int[] mask0 = env.getActionMask();
            int legal0 = 0; for (int v : mask0) if (v != 0) legal0++;
            System.out.printf("INITIAL — Phase: %s | turnOwner: %d | currentActor: %d | inProgress: %s%n",
                    env.getGamePhase(), env.getTurnOwner(), env.getCurrentActor(), env.getActionInProgressName());
            System.out.println("INITIAL legal actions: " + legal0);
            for (int i = 0; i < mask0.length; i++) {
                if (mask0[i] == 1) {
                    System.out.printf("  [%d] %s%n", i, env.getActionNameById(i));
                }
            }

            // --- Step loop ---
            for (int step = 1; step <= numberOfSteps; step++) {
                if (env.isDone()) {
                    System.out.println("\nEpisode finished before step " + step);
                    break;
                }

                System.out.printf("%n=== BEFORE step %d ===%n", step);
                // re-check head stability
                List<String> now = env.getLeafNames();
                if (now.size() != head.size()) {
                    throw new AssertionError("Leaf count changed! " + head.size() + " -> " + now.size());
                }
                for (int i = 0; i < head.size(); i++) {
                    if (!head.get(i).equals(now.get(i))) {
                        throw new AssertionError("Leaf order/name changed at " + i + ": '"
                                + head.get(i) + "' -> '" + now.get(i) + "'");
                    }
                }

                int[] maskBefore = env.getActionMask();
                int legalBefore = 0; for (int v : maskBefore) if (v != 0) legalBefore++;
                System.out.printf("Phase: %s | turnOwner: %d | currentActor: %d | inProgress: %s%n",
                        env.getGamePhase(), env.getTurnOwner(), env.getCurrentActor(), env.getActionInProgressName());
                System.out.println("BEFORE legal actions: " + legalBefore);
                for (int i = 0; i < maskBefore.length; i++) {
                    if (maskBefore[i] == 1) {
                        System.out.printf("  [%d] %s%n", i, env.getActionNameById(i));
                    }
                }

                // choose a random valid action
                int[] validIdx = java.util.stream.IntStream.range(0, maskBefore.length)
                        .filter(i -> maskBefore[i] == 1).toArray();
                if (validIdx.length == 0) {
                    System.out.println("⚠️  No valid actions; stopping.");
                    break;
                }
                int actionId = validIdx[rnd.nextInt(validIdx.length)];
                System.out.println("Chosen actionId: " + actionId + " -> " + env.getActionNameById(actionId));

                // Step
                env.step(actionId);

                // --- After step ---
                System.out.printf("%n=== AFTER step %d ===%n", step);
                System.out.println("Phase: " + env.getGamePhase()
                        + " | turnOwner: " + env.getTurnOwner()
                        + " | currentActor: " + env.getCurrentActor()
                        + " | inProgress: " + env.getActionInProgressName());
                int[] maskAfter = env.getActionMask();
                int legalAfter = 0; for (int v : maskAfter) if (v != 0) legalAfter++;
                System.out.println("AFTER legal actions: " + legalAfter);
                for (int i = 0; i < maskAfter.length; i++) {
                    if (maskAfter[i] == 1) {
                        System.out.printf("  [%d] %s%n", i, env.getActionNameById(i));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception during game initialisation: " + e);
            e.printStackTrace();
        }
    }

    
 // --- ADDED FOR DEBUG DELETE LATER ---
    
    public List<String> getLeafNames() {
        // same order as leaves/mask
        List<String> names = new ArrayList<>();
        for (ActionTreeNode leaf : this.leaves) names.add(leaf.getName());
        return names;
    }

    public AbstractAction getActionById(int id) {
        return this.leaves.get(id).getAction(); // may be null if masked off
    }

    public String getActionNameById(int id) {
        ActionTreeNode leaf = this.leaves.get(id);
        // Prefer a readable action string if bound, else the leaf name
        AbstractAction a = leaf.getAction();
        return (a != null) ? a.getString(gameState) : leaf.getName();
    }
    
    public String getGamePhase() {
        if (gameState instanceof games.powergrid.PowerGridGameState) {
            games.powergrid.PowerGridGameState s = (games.powergrid.PowerGridGameState) gameState;
            return s.getGamePhase().toString();
        }
        return "UNKNOWN";
    }
    
 // In your PyTAG helper/bridge class (not AbstractGameState)
    public int getCurrentActor() {
        return gameState.getCurrentPlayer();   // actor 'now', respects extended sequences
    }

    public int getTurnOwner() {
        return gameState.getTurnOwner();       // underlying turn owner
    }

    public String getActionInProgressName() {
        var aip = gameState.currentActionInProgress();
        return (aip == null) ? "NONE" : aip.toString();
    }

// --- ADDED FOR DEBUG DELETE LATER ---
 // Put this as a static nested helper or a small class in the same package
    final class PGProbe {
        static record Snap(
            // high-level status
            String phase,
            core.CoreConstants.GameResult gameStatus,
            int roundCounter, int turnCounter, int tick,

            // turn/actor
            int turnOwner, int currentPlayer, int turnOrderIndex,
            boolean actionInProgress, String topActionClass,

            // phase-specific flags
            boolean auctionLive,
            int step, int discountCard,
            int auctionPlant, int currentBid, int currentBidder,
            boolean roundOrderAllPassed,

            // orders
            List<Integer> turnOrder, List<Integer> roundOrder, List<Integer> bidOrder,

            // markets
            List<Integer> currentMkt, List<Integer> futureMkt,

            // per-player
            List<Integer> money, List<Integer> poweredCities, List<Integer> cityCounts,
            List<List<Integer>> ownedPlants, List<Map<String,Integer>> fuels,

            // board summary
            int activeRegions, int validCities, int invalidCities,
            int[] freeSlotsByStep // length 3: free slot counts in step 1/2/3
        ) {}

        static Snap snap(PowerGridGameState s) {
            // markets
            List<Integer> cur = s.getCurrentMarket()==null? List.of() :
                s.getCurrentMarket().getComponents().stream().map(PowerGridCard::getNumber).toList();
            List<Integer> fut = s.getFutureMarket()==null? List.of() :
                s.getFutureMarket().getComponents().stream().map(PowerGridCard::getNumber).toList();

            // orders
            List<Integer> to = new ArrayList<>(s.getTurnOrder());
            List<Integer> ro = new ArrayList<>(s.getRoundOrder());
            List<Integer> bo = new ArrayList<>(s.getBidOrder());

            // per-player snapshots
            int n = s.getNPlayers();
            List<Integer> money = new ArrayList<>(n);
            List<Integer> powered = new ArrayList<>(n);
            List<Integer> cities = new ArrayList<>(n);
            List<List<Integer>> owned = new ArrayList<>(n);
            List<Map<String,Integer>> fuels = new ArrayList<>(n);
            for (int p=0; p<n; p++) {
                money.add(s.getPlayersMoney(p));
                powered.add(s.getPoweredCities(p));
                cities.add(s.getCityCountByPlayer(p));

                var deck = s.getOwnedPlantsByPlayer(p);
                owned.add(deck==null ? List.of() :
                    deck.getComponents().stream().map(PowerGridCard::getNumber).toList());

                Map<String,Integer> fm = new LinkedHashMap<>();
                for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values())
                    fm.put(r.name(), s.getFuel(p, r));
                fuels.add(fm);
            }

            // board free slots summary
            int[] free = new int[]{0,0,0};
            int[][] slots = s.getCitySlotsById();
            if (slots != null) {
                for (int[] row : slots) {
                    for (int i=0; i<Math.min(3, row.length); i++)
                        if (row[i] == -1) free[i]++;
                }
            }

            // action stack
            boolean inProg = s.isActionInProgress();
            String top = inProg && s.currentActionInProgress()!=null
                    ? s.currentActionInProgress().getClass().getSimpleName()
                    : "NONE";

            return new Snap(
                String.valueOf(s.getGamePhase()),
                s.getGameStatus(),
                s.getRoundCounter(), s.getTurnCounter(), s.getGameTick(),

                s.getTurnOwner(), s.getCurrentPlayer(), s.getTurnOrderIndex(),
                inProg, top,

                s.isAuctionLive(),
                s.getStep(), s.getDiscountCard(),
                s.getAuctionPlantNumber(), s.getCurrentBid(), s.getCurrentBidder(),
                s.isRoundOrderAllPassed(),

                to, ro, bo,
                cur, fut,
                money, powered, cities, owned, fuels,
                s.getActiveRegions()==null?0:s.getActiveRegions().size(),
                s.getValidCities()==null?0:s.getValidCities().size(),
                s.getInvalidCities()==null?0:s.getInvalidCities().size(),
                free
            );
        }

        static void diff(String tag, Snap a, Snap b) {
            System.out.println("—— probe: " + tag + " ——");
            // print only when changed
            if (!Objects.equals(a.phase,b.phase))                    System.out.println("  phase        : "+a.phase+" → "+b.phase);
            if (a.gameStatus != b.gameStatus)                        System.out.println("  gameStatus   : "+a.gameStatus+" → "+b.gameStatus);
            if (a.roundCounter != b.roundCounter)                    System.out.println("  roundCounter : "+a.roundCounter+" → "+b.roundCounter);
            if (a.turnCounter != b.turnCounter)                      System.out.println("  turnCounter  : "+a.turnCounter+" → "+b.turnCounter);
            if (a.tick != b.tick)                                    System.out.println("  tick         : "+a.tick+" → "+b.tick);

            if (a.turnOwner != b.turnOwner)                          System.out.println("  turnOwner    : "+a.turnOwner+" → "+b.turnOwner);
            if (a.currentPlayer != b.currentPlayer)                  System.out.println("  currentActor : "+a.currentPlayer+" → "+b.currentPlayer);
            if (a.turnOrderIndex != b.turnOrderIndex)                System.out.println("  turnOrderIdx : "+a.turnOrderIndex+" → "+b.turnOrderIndex);
            if (a.actionInProgress != b.actionInProgress
             || !Objects.equals(a.topActionClass,b.topActionClass))  System.out.println("  inProgress   : "+a.topActionClass+" ("+a.actionInProgress+") → "+b.topActionClass+" ("+b.actionInProgress+")");

            if (a.auctionLive != b.auctionLive)                      System.out.println("  auctionLive  : "+a.auctionLive+" → "+b.auctionLive);
            if (a.step != b.step)                                    System.out.println("  step         : "+a.step+" → "+b.step);
            if (a.discountCard != b.discountCard)                    System.out.println("  discountCard : "+a.discountCard+" → "+b.discountCard);
            if (a.auctionPlant != b.auctionPlant)                    System.out.println("  auctionPlant : "+a.auctionPlant+" → "+b.auctionPlant);
            if (a.currentBid != b.currentBid)                        System.out.println("  currentBid   : "+a.currentBid+" → "+b.currentBid);
            if (a.currentBidder != b.currentBidder)                  System.out.println("  currentBidder: "+a.currentBidder+" → "+b.currentBidder);
            if (a.roundOrderAllPassed != b.roundOrderAllPassed)      System.out.println("  allPassed    : "+a.roundOrderAllPassed+" → "+b.roundOrderAllPassed);

            if (!Objects.equals(a.turnOrder,b.turnOrder))            System.out.println("  turnOrder    : "+a.turnOrder+" → "+b.turnOrder);
            if (!Objects.equals(a.roundOrder,b.roundOrder))          System.out.println("  roundOrder   : "+a.roundOrder+" → "+b.roundOrder);
            if (!Objects.equals(a.bidOrder,b.bidOrder))              System.out.println("  bidOrder     : "+a.bidOrder+" → "+b.bidOrder);

            if (!Objects.equals(a.currentMkt,b.currentMkt))          System.out.println("  currentMkt   : "+a.currentMkt+" → "+b.currentMkt);
            if (!Objects.equals(a.futureMkt,b.futureMkt))            System.out.println("  futureMkt    : "+a.futureMkt+" → "+b.futureMkt);

            if (!Objects.equals(a.money,b.money))                    System.out.println("  money        : "+a.money+" → "+b.money);
            if (!Objects.equals(a.poweredCities,b.poweredCities))    System.out.println("  powered      : "+a.poweredCities+" → "+b.poweredCities);
            if (!Objects.equals(a.cityCounts,b.cityCounts))          System.out.println("  cityCounts   : "+a.cityCounts+" → "+b.cityCounts);
            if (!Objects.equals(a.ownedPlants,b.ownedPlants))        System.out.println("  ownedPlants  : "+a.ownedPlants+" → "+b.ownedPlants);
            if (!Objects.equals(a.fuels,b.fuels))                    System.out.println("  fuels        : "+a.fuels+" → "+b.fuels);

            if (a.activeRegions != b.activeRegions
             || a.validCities  != b.validCities
             || a.invalidCities!= b.invalidCities)                   System.out.println("  regions/cities: act="+a.activeRegions+"→"+b.activeRegions+
                                                                                                   " valid="+a.validCities+"→"+b.validCities+
                                                                                                   " invalid="+a.invalidCities+"→"+b.invalidCities);
            if (!Arrays.equals(a.freeSlotsByStep,b.freeSlotsByStep)) System.out.println("  freeSlots    : "+Arrays.toString(a.freeSlotsByStep)+" → "+Arrays.toString(b.freeSlotsByStep));
        }
    }



}
