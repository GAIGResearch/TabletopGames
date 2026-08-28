package players.DUCT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import players.mcts.ActionStats;

import java.util.*;

import static utilities.Utils.noise;
import static utilities.Utils.normalise;

/**
 * A node in the DUCT tree. Main field is playerActionStats which holds stats per player per action,
 * so each player has its own UCB values. At a simultaneous node every acting player picks its own
 * action independently from its own stats, the picks are combined into one joint action, and the
 * child is stored under that joint action. That's the decoupling that makes it DUCT and not plain UCT.
 *
 * It is open loop: one state copy per iteration and the forward model changes it as we go down the tree.
 *
 * Who acts at a node comes straight from the game state via getCurrentSimultaneousPlayers(). The
 * game is the thing that knows whether one player or several are deciding in parallel, so we just
 * ask it each time rather than guessing from the action stack. If it gives us one player we advance
 * that single action, if it gives us several we build a joint action.
 */
public class DUCTNode {

    // stats per player, per action. outer key = player, inner key = an action. filled in lazily.
    final Map<Integer, Map<AbstractAction, ActionStats>> playerActionStats = new HashMap<>();

    // available actions for each acting player this iteration, refreshed each descent
    Map<Integer, List<AbstractAction>> playerCurrentActions = new HashMap<>();

    // children keyed by the joint action that leads to them
    final Map<AbstractAction, DUCTNode> children = new LinkedHashMap<>();

    final DUCTNode parent;   // null for the root
    final DUCTNode root;     // trajectory / budget / reward bounds live on the root
    final int depth;         // root == 0
    int nVisits;
    boolean terminalNode;
    final AbstractAction actionToReach;   // joint action taken from the parent to get here

    AbstractGameState state;          // master copy, root only
    AbstractGameState openLoopState;  // the state we are working on this iteration
    List<Integer> actingPlayers;      // players choosing at this node, as reported by the game state

    DUCTParams params;
    AbstractForwardModel forwardModel;
    Random rnd;

    // min/max reward seen, used for normalising (root only)
    double highReward = Double.NEGATIVE_INFINITY;
    double lowReward = Double.POSITIVE_INFINITY;

    int fmCallsCount;   // forward model calls so far (root only)

    // this iteration's path, rebuilt each time and kept on the root
    List<DUCTNode> currentNodeTrajectory;
    List<Map<Integer, AbstractAction>> currentActionTrajectory;

    // root constructor - keeps a master copy of the state to re-copy each iteration
    DUCTNode(DUCTParams params, AbstractForwardModel forwardModel,
             AbstractGameState state, Random rnd) {
        this.params = params;
        this.forwardModel = forwardModel;
        this.rnd = rnd;
        this.parent = null;
        this.root = this;
        this.depth = 0;
        this.actionToReach = null;
        this.nVisits = 0;
        this.state = state.copy();
        this.openLoopState = this.state;
        this.terminalNode = !state.isNotTerminal();
        this.currentNodeTrajectory = new ArrayList<>();
        this.currentActionTrajectory = new ArrayList<>();
        this.actingPlayers = computeActingPlayers(state);
        initialisePlayerActionStats(state);
    }

    // child constructor - no master copy, just takes the open loop state passed down
    private DUCTNode(DUCTNode parent, AbstractAction actionToReach,
                     AbstractGameState nextState) {
        this.parent = parent;
        this.root = parent.root;
        this.params = parent.params;
        this.forwardModel = parent.forwardModel;
        this.rnd = parent.rnd;
        this.depth = parent.depth + 1;
        this.actionToReach = actionToReach;
        this.nVisits = 0;
        this.state = null;
        this.openLoopState = nextState;
        this.terminalNode = !nextState.isNotTerminal();
        this.actingPlayers = computeActingPlayers(nextState);
        initialisePlayerActionStats(nextState);
    }

    // who chooses at this node. joint mode = all simultaneous players, otherwise just the current one.
    private List<Integer> computeActingPlayers(AbstractGameState gs) {
        // ask the game who is deciding here. decoupled=false is the ablation switch that forces
        // plain one-at-a-time UCT so we can measure what the decoupling is actually worth.
        if (params.decoupled) return gs.getCurrentSimultaneousPlayers();
        return Collections.singletonList(gs.getCurrentPlayer());
    }

    // make empty stats for every action each acting player has here
    private void initialisePlayerActionStats(AbstractGameState gs) {
        if (terminalNode) return;
        int nPlayers = gs.getNPlayers();
        for (int player : actingPlayers) {
            Map<AbstractAction, ActionStats> statsMap =
                    playerActionStats.computeIfAbsent(player, p -> new LinkedHashMap<>());
            for (AbstractAction action : forwardModel.computeAvailableActions(gs, params.actionSpace, player)) {
                statsMap.putIfAbsent(action, new ActionStats(nPlayers));
            }
        }
    }

    // sync the node to the current state: work out who acts + their actions, add any new actions
    // we haven't seen before (can happen after redeterminising)
    void updateForOpenLoopState(AbstractGameState gs) {
        this.openLoopState = gs;
        this.terminalNode = !gs.isNotTerminal();
        this.actingPlayers = computeActingPlayers(gs);

        playerCurrentActions = new HashMap<>();
        if (terminalNode) return;

        int nPlayers = gs.getNPlayers();
        for (int player : actingPlayers) {
            List<AbstractAction> actions =
                    forwardModel.computeAvailableActions(gs, params.actionSpace, player);
            playerCurrentActions.put(player, actions);

            Map<AbstractAction, ActionStats> statsMap =
                    playerActionStats.computeIfAbsent(player, p -> new LinkedHashMap<>());
            for (AbstractAction action : actions) {
                statsMap.putIfAbsent(action, new ActionStats(nPlayers));
            }
        }
    }

    // selection + expansion. at each node every acting player picks its own action by UCB, we join
    // them and advance, until we add a new child or hit a terminal / depth limit leaf.
    DUCTNode treePolicy() {
        DUCTNode cur = this;

        while (!cur.terminalNode && cur.depth < params.maxTreeDepth) {

          //  if (cur.actingPlayers.isEmpty() || cur.playerCurrentActions.isEmpty()) break;

            Map<Integer, AbstractAction> choices = cur.selectJointAction();
            if (choices.isEmpty()) throw new AssertionError("No actions available at " + cur);

            AbstractAction jointAction = buildJointAction(choices, cur.actingPlayers);

            // save the node + choices before we advance, backUp reads this in reverse
            root.currentNodeTrajectory.add(cur);
            root.currentActionTrajectory.add(choices);

            // next() changes openLoopState in place, copy the action so the key stays put
            forwardModel.next(cur.openLoopState, jointAction.copy());
            root.fmCallsCount++;

            DUCTNode child = cur.children.get(jointAction);

            if (child == null) {
                // new joint action here, make the child and roll out from it
                child = new DUCTNode(cur, jointAction.copy(), cur.openLoopState);
                cur.children.put(jointAction.copy(), child);
                return child;
            }

            child.updateForOpenLoopState(cur.openLoopState);
            cur = child;
        }

        return cur;
    }

    // each acting player independently picks its own action via UCB
    private Map<Integer, AbstractAction> selectJointAction() {
        Map<Integer, AbstractAction> choices = new LinkedHashMap<>();
        for (int player : actingPlayers) {
            List<AbstractAction> available =
                    playerCurrentActions.getOrDefault(player, Collections.emptyList());
            if (available.isEmpty()) throw new AssertionError("No actions available for player " + player);
            choices.put(player, selectActionForPlayer(player, available));
        }
        return choices;
    }

    // one acting player -> just its action. more than one -> wrap the picks in a SimultaneousAction.
    private AbstractAction buildJointAction(Map<Integer, AbstractAction> choices,
                                            List<Integer> acting) {
        if (acting.size() == 1) {
            return choices.get(acting.getFirst());
        }
        Map<Integer, AbstractAction> ordered = new HashMap<>();
        for (int player : acting) {
            AbstractAction a = choices.get(player);
            if (a != null) ordered.put(player, a);
        }
        return new SimultaneousAction(ordered);
    }

    // best UCB action for the player. shuffle first so ties get broken randomly (happens a lot
    // early when everything is on first play urgency).
    private AbstractAction selectActionForPlayer(int player, List<AbstractAction> available) {
        List<AbstractAction> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, rnd);

        Map<AbstractAction, ActionStats> statsMap =
                playerActionStats.getOrDefault(player, Collections.emptyMap());

        AbstractAction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (AbstractAction action : shuffled) {
            double ucb = computeUCB(player, action, statsMap);
            if (ucb > bestScore) {
                bestScore = ucb;
                best = action;
            }
        }

        if (best == null) throw new AssertionError("No actions available for player " + player);
        return best;
    }

    // UCB1 = mean value for this player + K * sqrt(log(N)/n). actions not tried yet get
    // firstPlayUrgency so they get picked first (also avoids log(0)).
    private double computeUCB(int player, AbstractAction action,
                              Map<AbstractAction, ActionStats> statsMap) {
        ActionStats stats = statsMap.get(action);

        if (stats == null || stats.nVisits == 0) {
            return params.firstPlayUrgency;
        }

        int n = stats.nVisits;
        int N = Math.max(1, nVisits);

        double q = stats.totValue[player] / n;

        // scale to [0,1] once we have some reward bounds from a rollout
        if (params.normaliseRewards && root.highReward > root.lowReward) {
            q = normalise(q, root.lowReward, root.highReward);
        }

        double exploration = params.K * Math.sqrt(Math.log(N) / n);

        return q + exploration;
    }

    // random playout from this node until terminal or we hit the rollout length, one player at a
    // time (the forward model sequences simultaneous players itself). gives back a score per player.
    double[] rollout() {
        AbstractGameState rolloutState = openLoopState;
        int steps = 0;

        while (rolloutState.isNotTerminal() && steps < params.rolloutLength) {
            int player = rolloutState.getCurrentPlayer();
            List<AbstractAction> actions =
                    forwardModel.computeAvailableActions(rolloutState, params.actionSpace, player);
            if (actions.isEmpty())
                throw new AssertionError("No actions available in rollout for player " + player);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            forwardModel.next(rolloutState, action);
            root.fmCallsCount++;
            steps++;
        }

        int nPlayers = rolloutState.getNPlayers();
        double[] result = new double[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            result[i] = params.heuristic.evaluateState(rolloutState, i);
        }
        return result;
    }

    // push the rollout scores back up the path. each player only updates its own action, they
    // only share the reward vector, not each others choices. thats the decoupling.
    void backUp(double[] delta) {
        updateNormalisationBounds(delta);

        List<DUCTNode> traj = root.currentNodeTrajectory;
        List<Map<Integer, AbstractAction>> actTraj = root.currentActionTrajectory;

        for (int i = traj.size() - 1; i >= 0; i--) {
            DUCTNode node = traj.get(i);
            Map<Integer, AbstractAction> choices = actTraj.get(i);

            node.nVisits++;

            for (Map.Entry<Integer, AbstractAction> entry : choices.entrySet()) {
                int player = entry.getKey();
                AbstractAction chosenAction = entry.getValue();

                Map<AbstractAction, ActionStats> statsMap = node.playerActionStats.get(player);
                if (statsMap == null) continue;

                ActionStats stats = statsMap.get(chosenAction);
                if (stats == null) continue;

                stats.update(delta);
            }
        }
    }

    private void updateNormalisationBounds(double[] delta) {
        if (!params.normaliseRewards) return;
        for (double v : delta) {
            if (v < root.lowReward) root.lowReward = v;
            if (v > root.highReward) root.highReward = v;
        }
    }

    // most visited action for the decision player, out of the ones still legal. noise breaks ties.
    // keeping to validActions avoids stale stats from redeterminised states.
    AbstractAction bestAction(int decisionPlayer, List<AbstractAction> validActions) {
        Map<AbstractAction, ActionStats> statsMap = playerActionStats.get(decisionPlayer);
        if (statsMap == null || statsMap.isEmpty()) {
            return validActions.get(rnd.nextInt(validActions.size()));
        }

        AbstractAction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (AbstractAction action : validActions) {
            ActionStats stats = statsMap.get(action);
            int visits = (stats != null) ? stats.nVisits : 0;
            double score = noise(visits, params.noiseEpsilon, rnd.nextDouble());
            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }

        return best != null ? best : validActions.get(rnd.nextInt(validActions.size()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("DUCTNode[depth=%d, visits=%d, children=%d]%n",
                depth, nVisits, children.size()));
        for (Map.Entry<Integer, Map<AbstractAction, ActionStats>> playerEntry
                : playerActionStats.entrySet()) {
            int player = playerEntry.getKey();
            Map<AbstractAction, ActionStats> statsMap = playerEntry.getValue();
            sb.append(String.format("  Player %d:%n", player));
            statsMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> -e.getValue().nVisits))
                    .limit(5)
                    .forEach(e -> {
                        ActionStats s = e.getValue();
                        double mean = s.nVisits > 0 ? s.totValue[player] / s.nVisits : 0.0;
                        sb.append(String.format("    %-40s visits=%d mean=%.4f%n",
                                e.getKey().toString(), s.nVisits, mean));
                    });
        }
        return sb.toString();
    }
}
