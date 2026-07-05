package players.DUCT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import players.mcts.ActionStats;

import java.util.*;

import static utilities.Utils.noise;
import static utilities.Utils.normalise;

/**
 * A node in the DUCT tree. Main field is playerActionStats wich holds stats per player per action,
 * so each player has its own UCB values. Thats what lets it handle simultaneous games.
 *
 * We advance the game one player at a time (the current player), same as the normal random
 * rollouts do, so we dont have to build a joint action. Its open loop: one state copy per
 * iteration and the forward model changes it as we go down the tree.
 */
public class DUCTNode {

    // stats per player, per action. outer key = player, inner key = an action. filled in lazily.
    final Map<Integer, Map<AbstractAction, ActionStats>> playerActionStats = new HashMap<>();

    // the current players avaliable actions for this iteration, refreshed each descent
    Map<Integer, List<AbstractAction>> playerCurrentActions = new HashMap<>();

    // children keyed by the action that leads to them
    final Map<AbstractAction, DUCTNode> children = new LinkedHashMap<>();

    final DUCTNode parent;   // null for the root
    final DUCTNode root;     // trajectory / budget / reward bounds live on the root
    final int depth;         // root == 0
    int nVisits;
    boolean terminalNode;
    final AbstractAction actionToReach;   // action taken from the parent to get here

    AbstractGameState state;          // master copy, root only
    AbstractGameState openLoopState;  // the state we are working on this iteration
    int actingPlayer;                 // player to move at this node

    DUCTParams params;
    AbstractForwardModel forwardModel;
    Random rnd;

    // min/max reward seen, used for normalising (root only)
    double highReward = Double.NEGATIVE_INFINITY;
    double lowReward = Double.POSITIVE_INFINITY;

    int fmCallsCount;   // forward model calls so far (root only)

    // this iterations path, rebuilt each time and kept on the root
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
        this.actingPlayer = state.getCurrentPlayer();
        this.currentNodeTrajectory = new ArrayList<>();
        this.currentActionTrajectory = new ArrayList<>();
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
        this.actingPlayer = nextState.getCurrentPlayer();
        initialisePlayerActionStats(nextState);
    }

    // make empty stats for every action the current player has here
    private void initialisePlayerActionStats(AbstractGameState gs) {
        if (terminalNode) return;
        int nPlayers = gs.getNPlayers();
        Map<AbstractAction, ActionStats> statsMap =
                playerActionStats.computeIfAbsent(actingPlayer, p -> new LinkedHashMap<>());
        for (AbstractAction action : forwardModel.computeAvailableActions(gs, params.actionSpace, actingPlayer)) {
            statsMap.putIfAbsent(action, new ActionStats(nPlayers));
        }
    }

    // sync the node to the current state: work out the current player + their actions, and add
    // any new actions we havent seen before (can happen after redeterminising)
    void updateForOpenLoopState(AbstractGameState gs) {
        this.openLoopState = gs;
        this.actingPlayer = gs.getCurrentPlayer();
        this.terminalNode = !gs.isNotTerminal();

        playerCurrentActions = new HashMap<>();
        if (terminalNode) return;

        int nPlayers = gs.getNPlayers();
        List<AbstractAction> actions =
                forwardModel.computeAvailableActions(gs, params.actionSpace, actingPlayer);
        playerCurrentActions.put(actingPlayer, actions);

        Map<AbstractAction, ActionStats> statsMap =
                playerActionStats.computeIfAbsent(actingPlayer, p -> new LinkedHashMap<>());
        for (AbstractAction action : actions) {
            statsMap.putIfAbsent(action, new ActionStats(nPlayers));
        }
    }

    // selection + expansion. keep letting the current player pick with UCB and advancing until we
    // add a new child or hit a terminal / depth limit leaf.
    DUCTNode treePolicy() {
        DUCTNode cur = this;

        while (!cur.terminalNode && cur.depth < params.maxTreeDepth) {

            List<AbstractAction> available = cur.playerCurrentActions.get(cur.actingPlayer);
            if (available == null || available.isEmpty()) break;

            int player = cur.actingPlayer;
            AbstractAction chosen = cur.selectActionForPlayer(player, available);

            // save the node + choice before we advance, backUp reads this in reverse
            root.currentNodeTrajectory.add(cur);
            root.currentActionTrajectory.add(Collections.singletonMap(player, chosen));

            // next() changes openLoopState in place, copy the action so the key stays put
            forwardModel.next(cur.openLoopState, chosen.copy());
            root.fmCallsCount++;

            DUCTNode child = cur.children.get(chosen);

            if (child == null) {
                // new action here, make the child and roll out from it
                child = new DUCTNode(cur, chosen.copy(), cur.openLoopState);
                cur.children.put(chosen.copy(), child);
                return child;
            }

            child.updateForOpenLoopState(cur.openLoopState);
            cur = child;
        }

        return cur;
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

        return best != null ? best : shuffled.get(0);
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
        if (params.normaliseRewards
                && root.highReward > root.lowReward
                && root.highReward != Double.NEGATIVE_INFINITY) {
            q = normalise(q, root.lowReward, root.highReward);
        }

        double exploration = params.K * Math.sqrt(Math.log(N) / n);

        return q + exploration;
    }

    // random playout from this node until terminal or we hit the rollout length, one player at a
    // time. gives back a score per player.
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

    int getVisits() { return nVisits; }

    int getDepth() { return depth; }

    Map<AbstractAction, ActionStats> getPlayerActionStats(int player) {
        return Collections.unmodifiableMap(
                playerActionStats.getOrDefault(player, Collections.emptyMap()));
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
