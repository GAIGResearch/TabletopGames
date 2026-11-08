package players.jsonBagPlayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.simple.RandomPlayer;

import java.util.*;

public class JSONBagOSLAPlayer extends AbstractPlayer {

    JSONBagHeuristic heuristic;
    int currentTurnCount = 0;
    int currentRoundCount = 0;

    boolean simultaneousMode = false;

    public JSONBagOSLAPlayer(List<Map<String, Integer>> prototypes, Collection<String> filterList, Random random) {
        super(null, "JSONBagOSLAPlayer");
        this.heuristic = new JSONBagHeuristic(prototypes, filterList);
        this.rnd = random;
    }

    public JSONBagOSLAPlayer(List<Map<String, Integer>> prototypes, List<String> filterList) {
       this(prototypes, filterList, new Random());
    }

    // For copying only
    private JSONBagOSLAPlayer(Random random) {
        super(null, "JSONBagOSLAPlayer");
        this.rnd = random;
    }

    public void setFilterList(Collection<String> filterList) {
        this.heuristic.setFilterSet(filterList);
    }

    public void setSimultaneousMode(boolean mode) { simultaneousMode = mode; }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {

        // Update current JSON Bag
        int newTurnCount = gs.getTurnCounter();
        int newRoundCount = gs.getRoundCounter();
        if (newTurnCount != currentTurnCount || newRoundCount != currentRoundCount)
        {
//            this.heuristic.updateJSONBag(gs);
            currentRoundCount = newRoundCount;
            currentTurnCount = newTurnCount;
        }
        this.heuristic.updateJSONBag(gs);
        Collections.shuffle(actions);   // making sure no artifacts causing certain action index always being picked

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        double[] valState = new double[actions.size()];
        int playerID = gs.getCurrentPlayer();

        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            AbstractAction action = actions.get(actionIndex);
            AbstractGameState gsCopy = gs.copy();
            rollNextGameState(gsCopy, action.copy());

            // JSON-bag eval here
            valState[actionIndex] = heuristic.evaluateState(gsCopy, playerID);

//            double Q = noise(valState[actionIndex], getParameters().noiseEpsilon, rnd.nextDouble());
            double Q = valState[actionIndex];

            if (Q > maxQ || bestAction == null) {
                maxQ = Q;
                bestAction = action;
            }
        }

        return bestAction;
    }

    // Override this when need to roll toward endRound or endTurn (not just one step)
    protected void rollNextGameState(AbstractGameState gs, AbstractAction a) {
        if (!simultaneousMode) {
            getForwardModel().next(gs, a);
        }
        else {
            // for simultaneous turn game (e.g., Wonders7)
            int currentRound = gs.getRoundCounter();
            getForwardModel().next(gs, a);
            RandomPlayer randomPlayer = new RandomPlayer(new Random(rnd.nextInt()));
            List<AbstractAction> actions;
            while (gs.getRoundCounter() == currentRound && gs.isNotTerminal()) {
                actions = getForwardModel().computeAvailableActions(gs);
                if (actions.isEmpty()) {
                    break;
                }
                getForwardModel().next(gs, randomPlayer.getAction(gs, actions));
            }
        }
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        heuristic.resetCurrentBag(gameState);
    }

    @Override
    public JSONBagOSLAPlayer copy() {
        JSONBagOSLAPlayer retValue = new JSONBagOSLAPlayer(new Random(rnd.nextInt()));
        retValue.heuristic = this.heuristic.copy();
        retValue.setForwardModel(getForwardModel());
        retValue.simultaneousMode = this.simultaneousMode;
        retValue.currentTurnCount = this.currentTurnCount;
        retValue.currentRoundCount = this.currentRoundCount;
        return retValue;
    }
}
