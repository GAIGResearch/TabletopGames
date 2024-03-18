package players.mcts;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static players.mcts.MCTSEnums.TreePolicy.RegretMatching;

public class MCTSSelectionTests {

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

    @Test
    public void ucb10Visits() {
        params.normaliseRewards = false;
        setupPlayer();

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
        double[] actionValues = node.actionValues(baseActions);
        assertEquals(0.0 + Math.sqrt(Math.log(10)), actionValues[0], 0.01);
        assertEquals(1.0 + Math.sqrt(Math.log(10)/5), actionValues[1], 0.01);
        assertEquals(1.4 / 1.5 + Math.sqrt(Math.log(10)/4), actionValues[2], 0.01);
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
        assertEquals(100, counts[0], 50);
        assertEquals(567, counts[1], 50);
        assertEquals(333, counts[2], 50);
    }


    @Test
    public void rm10VisitsWithNormalisation() {
        params.treePolicy = RegretMatching;
        params.exploreEpsilon = 0.0;
        params.normaliseRewards = true;
        setupPlayer();

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
}
