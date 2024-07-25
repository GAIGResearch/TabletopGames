package players.mcts;

import core.actions.AbstractAction;
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
        tenVisits(node, new double[]{-1.0, 0.5, 0.4});
        assertEquals(10, node.nVisits);
        assertEquals(1, node.getActionStats(new LMRAction("Left")).nVisits);
        assertEquals(5, node.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(4, node.getActionStats(new LMRAction("Right")).nVisits);
    }

    private void tenVisits(STNWithTestInstrumentation node, double[] rewards) {
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Left")));
        node.currentNodeTrajectory = List.of(node);
        node.backUp(new double[]{rewards[0]});

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        node.currentNodeTrajectory = List.of(node);
        for (int i = 0; i < 5; i++) {
            node.backUp(new double[]{rewards[1]});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        node.currentNodeTrajectory = List.of(node);
        for (int i = 0; i < 4; i++) {
            node.backUp(new double[]{rewards[2]});
        }
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
    public void rm10VisitsFinalSelection() {
        // we need this to be the same as the tree policy action after ten visits as
        // we only update every 10 visits
        params.treePolicy = RegretMatching;
        params.exploreEpsilon = 0.0;
        params.normaliseRewards = false;
        setupPlayer();
        first10Visits(node);

        int[] counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.bestAction();
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

        // this should then change after another 10 visits
        // we now make the third action much more attractive
        tenVisits(node, new double[]{0.0, 0.0, 1.0});
        counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.bestAction();
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        // We now have 100% Right after 20 visits
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals(0, counts[0], 0);
        assertEquals(680 / 2, counts[1], 100);
        assertEquals((320 + 1000) / 2, counts[2], 100);

        tenVisits(node, new double[]{4.0, 0.5, 0.4});
        counts = new int[3];
        for (int i = 0; i < 1000; i++) {
            AbstractAction action = node.bestAction();
            if (action.equals(new LMRAction("Left"))) {
                counts[0]++;
            } else if (action.equals(new LMRAction("Middle"))) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        // New policy is 84% Left, 16% Right...so we average that in with the previous two
        assertEquals(1000, counts[0] + counts[1] + counts[2]);
        assertEquals( (840)/3, counts[0], 50);
        assertEquals(680 / 3, counts[1], 50);
        assertEquals((320 + 1000 + 160) / 3, counts[2], 100);
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
    public void bias10Visits() {
        params.treePolicy = UCB;
        params.normaliseRewards = true;
        params.progressiveBias = 2.0;
        params.actionHeuristic = (a, s, l) -> {
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
        assertEquals(1.0 + Math.sqrt(Math.log(10) / 5), actionValues[1], 0.01);
        assertEquals(0.4 + 1.4 / 1.5 + Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
    }

    @Test
    public void pUCT10VisitsNoTemperature() {
        params.pUCT = true;
        params.pUCTTemperature = 0.0;
        params.actionHeuristic = (a, s, l) -> {
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

        // With pUCT, the exploration values are modified by the heuristic as follows:
        // Left: 0.3 / 1.0 = 0.3
        // Middle: 0.0 / 1.0 = 0.0
        // Right: 1.0 / 1.0 = 1.0
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.0 + 0.3 / 1.3 * Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0 + 0.0 * Math.sqrt(Math.log(10) / 5), actionValues[1], 0.01);
        assertEquals(1.4 / 1.5 + 1.0 / 1.3 * Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
    }

    @Test
    public void pUCT10VisitsWithTemperature() {
        params.pUCT = true;
        params.pUCTTemperature = 2.0;
        params.actionHeuristic = (a, s, l) -> {
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

        // With pUCT, the exploration values are modified by the heuristic as follows:
        // Left: exp(0.3/2) / N = 0.305
        // Middle: exp(0.0/2) / N = 0.262
        // Right: exp(1.0/2) / N = 0.433
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.0 + 0.305 * Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0 + 0.262 * Math.sqrt(Math.log(10) / 5), actionValues[1], 0.01);
        assertEquals(1.4 / 1.5 + 0.433 * Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
    }

    @Test
    public void actionSeeding10Visits() {
        params.initialiseVisits = 4;
        params.normaliseRewards = false;
        params.actionHeuristic = (a, s, l) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };
        setupPlayer();

        // For this test we don't use first10Visits() as the initial visit parameter changes the visit counts
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Left")));
        node.currentNodeTrajectory = List.of(node);
        node.backUp(new double[]{-1.0});

        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Middle")));
        node.currentNodeTrajectory = List.of(node);
        for (int i = 0; i < 5; i++) {
            node.backUp(new double[]{0.5});
        }
        node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
        node.currentNodeTrajectory = List.of(node);
        for (int i = 0; i < 4; i++) {
            node.backUp(new double[]{0.4});
        }

        assertEquals(22, node.nVisits); // 10, + 4 seed visits per action
        assertEquals(5, node.getActionStats(new LMRAction("Left")).nVisits);
        assertEquals(9, node.getActionStats(new LMRAction("Middle")).nVisits);
        assertEquals(8, node.getActionStats(new LMRAction("Right")).nVisits);

        assertEquals(22, node.getActionStats(new LMRAction("Left")).validVisits);
        assertEquals(22, node.getActionStats(new LMRAction("Middle")).validVisits);
        assertEquals(22, node.getActionStats(new LMRAction("Right")).validVisits);

        // With seeding, we change both the value of the action, and the exploration term
        double[] actionValues = node.actionValues(baseActions);
        assertEquals((-1.0 + 0.3 * 4) / 5.0 + Math.sqrt(Math.log(22) / 5), actionValues[0], 0.01);
        assertEquals(2.5 / 9.0 + Math.sqrt(Math.log(22) / 9), actionValues[1], 0.01);
        assertEquals((0.4 * 4 + 4.0) / 8.0 + Math.sqrt(Math.log(22) / 8), actionValues[2], 0.01);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
    }

    @Test
    public void progressiveWideningI() {
        params.progressiveWideningConstant = 1.0;
        params.progressiveWideningExponent = 0.5;
        params.actionHeuristic = (a, s, l) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };
        setupPlayer();

        // The second action should become available at the 4th visits (2 = sqrt(N))
        for (int i = 0; i < 3; i++) {
            // Check that the correct number of actions are available (just the one)
            //      System.out.println("Actions: " + node.actionsToConsider(node.actionsFromOpenLoopState));
            assertEquals(3, node.actionsFromOpenLoopState.size());
            assertEquals(1, node.actionsToConsider(node.actionsFromOpenLoopState).size());
            assertEquals(new LMRAction("Right"), node.actionsToConsider(node.actionsFromOpenLoopState).get(0));
            node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
            node.currentNodeTrajectory = List.of(node);
            node.backUp(new double[]{-1.0});

            // then check that only the available actions are updated
            assertEquals(i + 1, node.getActionStats(new LMRAction("Right")).validVisits);
            assertEquals(i == 2 ? 1 : 0, node.getActionStats(new LMRAction("Left")).validVisits);
            assertEquals(0, node.getActionStats(new LMRAction("Middle")).validVisits);
        }
        assertEquals(3, node.actionsFromOpenLoopState.size());
        assertEquals(2, node.actionsToConsider(node.actionsFromOpenLoopState).size());
        assertTrue(node.actionsToConsider(node.actionsFromOpenLoopState).contains(new LMRAction("Left")));
        assertTrue(node.actionsToConsider(node.actionsFromOpenLoopState).contains(new LMRAction("Right")));

        // The third action should become available at the 9th visits (3 = sqrt(N))
        for (int i = 3; i < 8; i++) {
            // Check that the correct number of actions are available (just the one)
            // System.out.println("Actions: " + node.actionsToConsider(node.actionsFromOpenLoopState));
            assertEquals(3, node.actionsFromOpenLoopState.size());
            assertEquals(2, node.actionsToConsider(node.actionsFromOpenLoopState).size());
            node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Left")));
            node.backUp(new double[]{-1.0});
        }
        assertEquals(3, node.actionsFromOpenLoopState.size());
        assertEquals(3, node.actionsToConsider(node.actionsFromOpenLoopState).size());
        assertTrue(node.actionsToConsider(node.actionsFromOpenLoopState).contains(new LMRAction("Left")));
        assertTrue(node.actionsToConsider(node.actionsFromOpenLoopState).contains(new LMRAction("Right")));
        assertTrue(node.actionsToConsider(node.actionsFromOpenLoopState).contains(new LMRAction("Middle")));
    }


    @Test
    public void progressiveWideningII() {
        params.progressiveWideningConstant = 2.0;
        params.progressiveWideningExponent = 0.1;
        params.actionHeuristic = (a, s, l) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };
        setupPlayer();

        // The third action should become available at the 58th visit (3/2)^10
        for (int i = 0; i < 57; i++) {
            // Check that the correct number of actions are available (just the one)
            // System.out.println("Actions: " + node.actionsToConsider(node.actionsFromOpenLoopState));
            assertEquals(3, node.actionsFromOpenLoopState.size());
            assertEquals(2, node.actionsToConsider(node.actionsFromOpenLoopState).size());
            assertEquals(new LMRAction("Right"), node.actionsToConsider(node.actionsFromOpenLoopState).get(0));
            assertEquals(new LMRAction("Left"), node.actionsToConsider(node.actionsFromOpenLoopState).get(1));
            node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
            node.currentNodeTrajectory = List.of(node);
            node.backUp(new double[]{-1.0});
        }
        assertEquals(3, node.actionsToConsider(node.actionsFromOpenLoopState).size());
    }

    @Test
    public void progressiveWideningIII() {
        // check that only the available actions are used up to the point of widening
        params.progressiveWideningConstant = 1.0;
        params.progressiveWideningExponent = 0.2;
        params.actionHeuristic = (a, s, l) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };
        setupPlayer();

        // The second action should become available at the 32nd visit (2 =  N^(1/5))
        for (int i = 0; i < 31; i++) {
            // Check that the correct number of actions are available (just the one)
            // System.out.println("Actions: " + node.actionsToConsider(node.actionsFromOpenLoopState));
            assertEquals(1, node.actionsToConsider(node.actionsFromOpenLoopState).size());
            assertEquals(new LMRAction("Right"), node.treePolicyAction(true));
            node.actionsInTree = List.of(new Pair<>(0, new LMRAction("Right")));
            node.currentNodeTrajectory = List.of(node);
            node.backUp(new double[]{1.0});
        }
        assertEquals(2, node.actionsToConsider(node.actionsFromOpenLoopState).size());
        assertEquals(new LMRAction("Left"), node.treePolicyAction(true));
        // Actions are only formally available for the visits where they are considered
        assertEquals(1, node.getActionStats(new LMRAction("Left")).validVisits);
        assertEquals(31, node.getActionStats(new LMRAction("Right")).validVisits);
        assertEquals(0, node.getActionStats(new LMRAction("Middle")).validVisits);
    }

    @Test
    public void fpuWithpUCT() {
        // Check that pUCT is applied after FPU, so that we get effective pruning of values
        params.firstPlayUrgency = 2.0;
        params.pUCT = true;
        params.pUCTTemperature = 0.0;
        params.actionHeuristic = (a, s, l) -> {
            if (a.equals(new LMRAction("Left"))) {
                return 0.3;
            } else if (a.equals(new LMRAction("Middle"))) {
                return 0.0;
            } else {
                return 1.0;
            }
        };
        setupPlayer();

        double[] actionValues = node.actionValues(baseActions);
        assertEquals(2.0 * 0.3 / 1.3, actionValues[0], 0.01);
        assertEquals(0.0, actionValues[1], 0.01);
        assertEquals(2.0 / 1.3, actionValues[2], 0.01);

        first10Visits(node);
        assertEquals(new LMRAction("Right"), node.treePolicyAction(false));
        actionValues = node.actionValues(baseActions);
        assertEquals(0.0 + 0.3 / 1.3 * Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0, actionValues[1], 0.01);
        assertEquals(1.4 / 1.5 + 1.0 / 1.3 * Math.sqrt(Math.log(10) / 4), actionValues[2], 0.01);
    }

    @Test
    public void uniformSelection() {
        params.treePolicy = Uniform;
        setupPlayer();
        first10Visits(node);
        //regardless of the data, we expect a uniform distribution
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
        assertEquals(333, counts[0], 75);
        assertEquals(333, counts[1], 75);
        assertEquals(333, counts[2], 75);
    }


    @Test
    public void greedySelection() {
        params.treePolicy = Greedy;
        params.exploreEpsilon = 0.21;
        setupPlayer();
        first10Visits(node);
        //  we expect a 21% exploration (7% each)
        // and then 80% focused on the greedy Middle option
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
        assertEquals(70, counts[0], 25);
        assertEquals(860, counts[1], 75);
        assertEquals(70, counts[2], 25);
    }
}
