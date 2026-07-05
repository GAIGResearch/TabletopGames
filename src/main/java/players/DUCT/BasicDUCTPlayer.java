package players.DUCT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.ElapsedCpuTimer;

import java.util.*;

import static players.PlayerConstants.*;


public class BasicDUCTPlayer extends AbstractPlayer {

    // the search tree, rebuilt every turn
    protected DUCTNode root;

    public BasicDUCTPlayer() {
        this(new DUCTParams(), "BasicDUCTPlayer");
    }

    public BasicDUCTPlayer(DUCTParams params) {
        this(params, "BasicDUCTPlayer");
    }

    public BasicDUCTPlayer(DUCTParams params, String name) {
        super(params, name);
        rnd = new Random(parameters.getRandomSeed());
        // handle single action states ourselves so getAction doesnt skip _getAction
        considerSingletonActions = true;
    }

    @Override
    public DUCTParams getParameters() {
        return (DUCTParams) parameters;
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        // clear the old tree from the last game
        root = null;
        if (getParameters().resetSeedEachGame) {
            rnd = new Random(parameters.getRandomSeed());
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState,
                                     List<AbstractAction> possibleActions) {

        DUCTParams params = getParameters();

        // root keeps the master state, copied again each iteration
        root = new DUCTNode(params, getForwardModel(), gameState, rnd);
        root.updateForOpenLoopState(root.state);

        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        int numIters = 0;
        boolean stop = false;
        int remainingLimit = params.breakMS;

        while (!stop) {
            getForwardModel().reset();

            // if redeterminise is on, take a fresh guess of the hidden cards each time so we
            // dont just search one fixed guess. otherwise reuse the same state.
            AbstractGameState iterationState = params.redeterminise
                    ? gameState.copy(getPlayerID())
                    : root.state.copy();
            root.openLoopState = iterationState;
            root.updateForOpenLoopState(iterationState);

            root.currentNodeTrajectory.clear();
            root.currentActionTrajectory.clear();

            DUCTNode selected = root.treePolicy();   // selection + expansion
            double[] delta = selected.rollout();      // random playout, one score per player
            root.backUp(delta);                       // update each players stats on its own

            numIters++;
            switch (params.budgetType) {
                case BUDGET_TIME -> {
                    long remaining = elapsedTimer.remainingTimeMillis();
                    double avgTimeTaken = (double) elapsedTimer.elapsedMillis() / numIters;
                    stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
                }
                case BUDGET_ITERATIONS -> stop = numIters >= params.budget;
                case BUDGET_FM_CALLS -> stop = root.fmCallsCount >= params.budget
                        || numIters >= params.budget;
                default -> stop = numIters >= params.budget;
            }
        }

        // only look at moves that are actually legal right now
        AbstractAction best = root.bestAction(getPlayerID(), possibleActions);
        return best.copy();
    }

    @Override
    public void setForwardModel(AbstractForwardModel model) {
        super.setForwardModel(model);
    }

    @Override
    public BasicDUCTPlayer copy() {
        BasicDUCTPlayer copy = new BasicDUCTPlayer(
                (DUCTParams) getParameters().copy(), toString());
        if (getForwardModel() != null) {
            copy.setForwardModel(getForwardModel());
        }
        return copy;
    }

    // the tree from the last search, or null if none has run
    public DUCTNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
