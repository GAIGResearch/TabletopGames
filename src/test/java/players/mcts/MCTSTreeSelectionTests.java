package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import org.junit.Test;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static players.mcts.MCTSEnums.TreePolicy.*;

public class MCTSTreeSelectionTests {

    LMRForwardModel fm = new LMRForwardModel();
    LMRGame game = new LMRGame(new LMTParameters(302));
    MCTSParams params = new MCTSParams();
    TestMCTSPlayer player;
    Random rnd = new Random(303897);
    STNWithTestInstrumentation node;


    List<AbstractAction> baseActions = List.of(new LMRAction("Left"), new LMRAction("Middle"), new LMRAction("Right"));

    public void setupPlayer() {
        fm.setup(game);
        player = new TestMCTSPlayer(params, STNWithTestInstrumentation::new);
        player.setForwardModel(fm);
        node = (STNWithTestInstrumentation) SingleTreeNode.createRootNode(player, game, rnd, STNWithTestInstrumentation::new);

    }

    @Test
    public void testFPU() {
        params.firstPlayUrgency = 20000;
        params.normaliseRewards = false;
        setupPlayer();

        double[] results = node.actionValues(baseActions);
        assertEquals(3, results.length);
        assertEquals(20000, results[0], 0.001);
        assertEquals(20000, results[1], 0.001);
        assertEquals(20000, results[2], 0.001);

        node.backUpSingleNode(new LMRAction("Middle"), new double[]{1.0});
        double exploration = Math.sqrt(Math.log(1));
        results = node.actionValues(baseActions);
        assertEquals(3, results.length);
        assertEquals(20000, results[0], 0.001);
        // With only one result, this is normalised to 0.0
        assertEquals(1.0 + exploration, results[1], 0.001);
        assertEquals(20000, results[2], 0.001);

        node.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        assertEquals(new LMRAction("Right"), node.treePolicyAction(true));
    }


    private void first10Visits(STNWithTestInstrumentation node) {
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Left")));
        node.backUp(new double[]{-1.0});

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        for (int i = 0; i < 5; i++) {
            node.backUp(new double[]{0.5});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        for (int i = 0; i < 4; i++) {
            node.backUp(new double[]{0.4});
        }

        assertEquals(10, node.nVisits);
        assertEquals(1, node.getActionStats(new LMRAction("Left")).nVisits);
        assertEquals(5, node.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(4, node.getActionStats(new LMRAction("Right")).nVisits);
    }

    @Test
    public void ucb10Visits() {
        params.normaliseRewards = false;
        setupPlayer();
        first10Visits(node);

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.52, actionValues[0], 0.01);
        assertEquals(1.18, actionValues[1], 0.01);
        assertEquals(1.16, actionValues[2], 0.01);
        assertEquals(new LMRAction("Middle"), node.treePolicyAction(true));
    }

    @Test
    public void ucb10VisitsNormalised() {
        params.normaliseRewards = true;
        setupPlayer();
        first10Visits(node);

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.0 + Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0 + Math.sqrt(Math.log(10) / 5), actionValues[1], 0.01);
        assertEquals(1.4 / 1.5 + Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(true));
        // The recommendation changes because the effective size of K has been changed (increased)
    }

    @Test
    public void ucb20Visits() {
        ucb10Visits();

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        for (int i = 0; i < 5; i++) {
            node.backUp(new double[]{0.5});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        for (int i = 0; i < 5; i++) {
            node.backUp(new double[]{0.4});
        }

        assertEquals(20, node.nVisits);
        assertEquals(1, node.getActionStats(new LMRAction("Left")).nVisits);
        assertEquals(10, node.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(9, node.getActionStats(new LMRAction("Right")).nVisits);
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.73, actionValues[0], 0.01);
        assertEquals(1.05, actionValues[1], 0.01);
        assertEquals(0.98, actionValues[2], 0.01);
        assertEquals(new LMRAction("Middle"), node.treePolicyAction(true));
    }

    @Test
    public void ucb40Visits() {
        ucb20Visits();

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        for (int i = 0; i < 13; i++) {
            node.backUp(new double[]{0.5});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        for (int i = 0; i < 7; i++) {
            node.backUp(new double[]{0.4});
        }
        assertEquals(40, node.nVisits);
        assertEquals(1, node.getActionStats(new LMRAction("Left")).nVisits);
        assertEquals(23, node.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(16, node.getActionStats(new LMRAction("Right")).nVisits);
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.92, actionValues[0], 0.01);
        assertEquals(0.90, actionValues[1], 0.01);
        assertEquals(0.88, actionValues[2], 0.01);
        assertEquals(new LMRAction("Left"), node.treePolicyAction(true));
    }

    @Test
    public void rm10Visits() {
        params.treePolicy = RegretMatching;
        params.exploreEpsilon = 0.0;
        params.normaliseRewards = false;
        setupPlayer();
        first10Visits(node);

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.00, actionValues[0], 0.01);
        //   assertEquals(0.19, actionValues[1], 0.01);
        // The Regret just needs to have the right ratio for the two options with positive regret
        assertEquals(0.09 * actionValues[1] / 0.19, actionValues[2], 0.01);
        // The final choice of action is then stochastic
        int[] counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.treePolicyAction(true);
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals(0, counts[0], 0);
        assertEquals(679, counts[1], 100);
        assertEquals(321, counts[2], 100);
    }


    @Test
    public void rm10VisitsWithExploration() {
        params.treePolicy = RegretMatching;
        params.exploreEpsilon = 0.3;
        params.normaliseRewards = false;
        setupPlayer();
        first10Visits(node);

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.00, actionValues[0], 0.01);
        //   assertEquals(0.19, actionValues[1], 0.01);
        // The Regret just needs to have the right ratio for the two options with positive regret
        assertEquals(0.09 * actionValues[1] / 0.19, actionValues[2], 0.01);
        // The final choice of action is then stochastic
        int[] countsWithExploration = new int[3];
        int[] countsWithoutExploration = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction exploreAction = node.treePolicyAction(true);
            if (exploreAction.equals(new LMRAction("Left"))) {
                countsWithExploration[0]++;
            } else if (exploreAction.equals(new LMRAction("Middle"))) {
                countsWithExploration[1]++;
            } else {
                countsWithExploration[2]++;
            }
            AbstractAction bestAction = node.treePolicyAction(false);
            if (bestAction.equals(new LMRAction("Left"))) {
                countsWithoutExploration[0]++;
            } else if (bestAction.equals(new LMRAction("Middle"))) {
                countsWithoutExploration[1]++;
            } else {
                countsWithoutExploration[2]++;
            }
        }
        assertEquals(1000, countsWithExploration[0] + countsWithExploration[1] + countsWithExploration[2]);
        assertEquals(100, countsWithExploration[0], 50);
        assertEquals(611, countsWithExploration[1], 50);
        assertEquals(289, countsWithExploration[2], 50);

        assertEquals(1000, countsWithoutExploration[0] + countsWithoutExploration[1] + countsWithoutExploration[2]);
        assertEquals(0, countsWithoutExploration[0], 0);
        assertEquals(679, countsWithoutExploration[1], 50);
        assertEquals(321, countsWithoutExploration[2], 50);
    }


    @Test
    public void rm10VisitsWithNormalisation() {
        params.treePolicy = RegretMatching;
        params.exploreEpsilon = 0.0;
        params.normaliseRewards = true;
        setupPlayer();
        first10Visits(node);

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.00, actionValues[0], 0.01);
        //   assertEquals(0.19, actionValues[1], 0.01);
        // The Regret just needs to have the right ratio for the two options with positive regret
        assertEquals(0.09 * actionValues[1] / 0.19, actionValues[2], 0.01);
        // The final choice of action is then stochastic
        int[] counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.treePolicyAction(true);
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals(0, counts[0], 0);
        assertEquals(679, counts[1], 100);
        assertEquals(321, counts[2], 100);
    }


    @Test
    public void rm40Visits() {
        rm10Visits();

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        for (int i = 0; i < 18; i++) {
            node.backUp(new double[]{0.5});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        for (int i = 0; i < 12; i++) {
            node.backUp(new double[]{0.4});
        }

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.0, actionValues[0], 0.01);
        assertTrue(actionValues[1] > 0.0);
        assertEquals(0.0, actionValues[2], 0.01);
        assertEquals(new LMRAction("Middle"), node.treePolicyAction(true));
    }


    @Test
    public void exp3_10Visits() {
        params.treePolicy = EXP3;
        params.exploreEpsilon = 0.3;
        params.normaliseRewards = true;
        params.exp3Boltzmann = 0.80;
        setupPlayer();
        first10Visits(node);
        // When normalised to [0, 1], we expect
        // Left: 0.0
        // Middle: 1.0
        // Right: 0.9333

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(Math.exp(0.0), actionValues[0], 0.01);  // 1.0
        assertEquals(Math.exp(1.0 / 0.80), actionValues[1], 0.01); // 3.49
        assertEquals(Math.exp(0.93333 / 0.80), actionValues[2], 0.01); // 3.21
        // The final choice of action is then stochastic
        int[] counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.treePolicyAction(true);
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        // expected probability distribution is 13%, 45%, 42%  [not including the 30% exploration]
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals(0.13 * 700 + 100, counts[0], 50);
        assertEquals(0.45 * 700 + 100, counts[1], 50);
        assertEquals(0.42 * 700 + 100, counts[2], 50);
    }


    @Test
    public void hedge_10Visits() {
        params.treePolicy = Hedge;
        params.exploreEpsilon = 0.1;
        params.normaliseRewards = false;
        params.hedgeBoltzmann = 0.8;
        setupPlayer();
        first10Visits(node);

        // Regrets are:
        // -1.31 / 0.19 / 0.09

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(Math.exp(-1.31 * 10 / 0.8), actionValues[0], 0.01);  // 0.00
        assertEquals(Math.exp(0.19 * 10 / 0.80), actionValues[1], 0.01); // 10.75
        assertEquals(Math.exp(0.09 * 10 / 0.80), actionValues[2], 0.01); // 3.08
        // The final choice of action is then stochastic
        int[] counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.treePolicyAction(true);
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        // expected probability distribution is 0%, 78%, 22%  [not including the 10% exploration]
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals(0.00 * 900 + 33, counts[0], 15);
        assertEquals(0.78 * 900 + 33, counts[1], 50);
        assertEquals(0.22 * 900 + 33, counts[2], 50);
    }

    @Test
    public void bias10Visits() {
        params.treePolicy = UCB;
        params.normaliseRewards = true;
        params.progressiveBias = 2.0;
        params.actionHeuristic = (a, s) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };

        setupPlayer();
        first10Visits(node);

        // With progressive bias, the action values are modified by the heuristic as follows:
        // Left: +0.6 / 2 + 0.0 = 0.3
        // Middle: +0.0 / 6 + 1.0 = 1.0
        // Right: + 2.0 / 5 + XX = 0.4 + XX
        // These should be added after the normalisation (otherwise normalisation at the start could swing the result far too much later in the search)
        // And the normalisation only takes place using the actual back-propagated rewards
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.3 + Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0 + Math.sqrt(Math.log(10) / 5) , actionValues[1], 0.01);
        assertEquals(0.4 + 1.4 / 1.5 + Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
    }

}
