package groupM.players.breadthFirst;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.*;

import static players.PlayerConstants.BUDGET_TIME;

public class BreadthFirstPlayer extends AbstractPlayer {

    BreadthFirstParams params;

    Map<AbstractGameState, BreadthFirstTreeNode> stateToNodeMap = new HashMap<AbstractGameState, BreadthFirstTreeNode>();

    public BreadthFirstPlayer(BreadthFirstParams params) {
        this.params = params;
        setName("Breadth First");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();  // New timer for this game tick
        timer.setMaxTimeMillis(params.budget);
        double acumTimeTaken = 0;

        long remaining;
        int remainingLimit = this.params.breakMS;

        // Search for best action from the root
        BreadthFirstTreeNode root = new BreadthFirstTreeNode(this, null, gameState);

        Queue<BreadthFirstTreeNode> queue = new LinkedList<BreadthFirstTreeNode>();
        BreadthFirstTreeNode current = root;

        boolean stop = false;
        // Check stopping condition
        PlayerConstants budgetType = this.params.budgetType;

        while (!stop) {
            queue.addAll(current.expand());
            double delta = current.rollOut();

            // Back up the value of the rollout through the tree. We still do this regardless of above because we might
            // because we might have reached this state through a different route.
            current.backUp(delta);

            current = queue.poll();

            if (budgetType == BUDGET_TIME) {
                // Time budget
                stop = timer.remainingTimeMillis() <= remainingLimit;
            }
            if (current == null) {
                stop = true;
            }
        }

        return root.bestAction();
    }

    @Override
    public String toString() {
        return "BreadthFirst";
    }

    @Override
    public BreadthFirstPlayer copy() {
        return this;
    }
}
