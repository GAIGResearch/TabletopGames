package players.mcts;

import core.actions.AbstractAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The candidate actions, and the statistics over them, for ONE player at ONE tree node.
 * <p>
 * These five pieces of state co-vary: they are written together as a node is (re)visited in
 * {@link SingleTreeNode#setActionsFromOpenLoopState}, and read together by the tree policy on the
 * way back down. Holding them in one object is what lets a node carry a set of them:
 * one per simultaneously-moving player at a multi-actor node of a decoupled
 * search, and exactly one, for its own decisionPlayer, at every node of a sequential one.
 * <p>
 * This is a data holder with no behaviour and no back-reference to the node. The selection policy
 * stays on {@link SingleTreeNode}, where it also needs params, the root's reward bounds, the parent
 * and the visit count.
 * <p>
 * The map implementations are load-bearing and must not be "tidied". Plain {@code HashMap} with no
 * initial-capacity argument: the iteration order that results is observable, because
 * {@code nodeValue} sums over {@code values()} - so the order fixes the floating-point summation,
 * and can flip a UCB comparison. A LinkedHashMap, a TreeMap, or a presized HashMap all change it.
 */
public class PlayerDecisionStats {

    /** The player whose decision these statistics describe. */
    final int player;

    /** The actions available to that player on the current iteration. Replaced wholesale each visit. */
    List<AbstractAction> actionsFromOpenLoopState = new ArrayList<>();

    /** Visits and values per candidate action. */
    final Map<AbstractAction, ActionStats> actionValues = new HashMap<>();

    /** Action-heuristic scores, used for move ordering and progressive bias. */
    final Map<AbstractAction, Double> actionValueEstimates = new HashMap<>();

    /** The pUCT prior. Rebuilt wholesale whenever the pdf is recalculated. */
    Map<AbstractAction, Double> actionPDFEstimates = new HashMap<>();

    /** The regret-matching average policy. */
    final Map<AbstractAction, Double> regretMatchingAverage = new HashMap<>();

    PlayerDecisionStats(int player) {
        this.player = player;
    }
}
