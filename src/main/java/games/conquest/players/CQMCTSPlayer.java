package games.conquest.players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import games.conquest.CQGameState;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;

import java.util.List;
import java.util.Random;

public class CQMCTSPlayer extends AbstractPlayer {
    CQTreeNode root = null;
    int totalBudgetUsed = 0;
    int totalMovesMade = 0;

    public CQMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public CQMCTSPlayer(long seed) {
        this(seed, "Conquest MCTS");
    }
    public CQMCTSPlayer(long seed, String name) {
        super(new CQMCTSParams(), name);
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        CQMCTSParams params = getParameters();
        params.K = Math.sqrt(2);
        params.maxTreeDepth = 5;
        params.epsilon = 1e-6;
        params.flexibleBudget = true;
        params.rolloutLength = 0;
    }

    public CQMCTSPlayer(CQMCTSParams params) {
        super(params, "CQ MCTS");
    }

    public CQMCTSParams getParameters() {
        return (CQMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().heuristic = heuristic;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Get the root node if it exists already, or create a new one if it doesn't.
     * @param gameState
     * @return
     */
    void createRoot(AbstractGameState gameState) {
        if (root == null)
            root = new CQTreeNode(this, null, gameState, rnd);
        else if (!root.getState().equals(gameState)) {
            System.out.println("Node state not equal to given game state!");
            System.out.println(root.getState().equals(gameState));
            root = new CQTreeNode(this, null, gameState, rnd);
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        createRoot(gameState); // if root doesn't exist right now, create it
        if (root.depth == 0) {
            if (!gameState.getGamePhase().equals(CQGameState.CQGamePhase.SelectionPhase) && ((CQGameState) gameState).getTroops(gameState.getCurrentPlayer()).size() != 1)
                if (((CQGameState) gameState).getTroops(gameState.getCurrentPlayer()).size() == 2) {
                    for (Troop troop : ((CQGameState) gameState).getTroops(gameState.getCurrentPlayer())) {
                        if (!troop.hasCommand(CommandType.Chastise) && !troop.equals(((CQGameState) gameState).getSelectedTroop()))
                            // somehow not chastised and not the selected troop, which is not supposed to happen
                            System.out.println("Phase is not selection phase while there are multiple remaining troops!");
                    }
                } else {
                    System.out.println("Phase is not selection phase while there are multiple remaining troops!");
                }
            root.mctsSearch(getParameters().flexibleBudget);
        } else if (root.children.size() > 1 && (
                gameState.getGamePhase().equals(CQGameState.CQGamePhase.CombatPhase) ||
                gameState.getGamePhase().equals(CQGameState.CQGamePhase.RallyPhase)
        )) {
            root.depth = 0;
            root.parent = null;
            root.checkCommandActivation();
        }
        AbstractAction best = root.greedy();
        if (best == null) {
            System.out.println("No best action... what?");
        }
        // Now prepare the player for the next move
        root = root.children.get(best);
        totalBudgetUsed += root.root.totalBudgetUsed;
        totalMovesMade++; // every time _getAction is called, a single move is decided, even if we computed it during our previous move.
        while (root != null && root.children.size() == 1) {
            // if there is only a single action after this, _getAction will not be called.
            // Skip forward until we reach a point where multiple actions can be taken.
            root = root.children.values().iterator().next();
        }
        if (root != null && (this.getPlayerID() != root.playerId || !root.getState().isNotTerminal())) {
            // It's not the current player's turn up next, so clean up the root that's based on this player.
            root = null;
        }
        if (!possibleActions.contains(best)) {
            System.out.println("Not possible to play this action");
        }
        return best;
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
        super.finalizePlayer(gameState);
        System.out.printf("Game finished with a total budget of %d, used in %d moves, for an average of %.2f per move.", totalBudgetUsed, totalMovesMade, ((double)totalBudgetUsed / (double)totalMovesMade));
        gameState.recordHistory(String.format("Player %d finished with a total budget of %d, used in %d moves, for an average of %.2f per move.", getPlayerID(), totalBudgetUsed, totalMovesMade, ((double)totalBudgetUsed / (double)totalMovesMade)));
    }

    @Override
    public CQMCTSPlayer copy() {
        return this;
    }
}
