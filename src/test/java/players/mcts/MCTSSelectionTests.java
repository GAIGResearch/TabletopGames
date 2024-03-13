package players.mcts;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

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

        node.backUpSingleNode(new LMRAction("Left"), new double[]{-1.0});
        for (int i = 0; i < 5; i++) {
            node.backUpSingleNode(new LMRAction("Middle"), new double[]{0.5});
        }
        for (int i = 0; i < 4; i++) {
            node.backUpSingleNode(new LMRAction("Right"), new double[]{0.4});
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

        node.backUpSingleNode(new LMRAction("Left"), new double[]{-1.0});

        for (int i = 0; i < 5; i++) {
            node.backUpSingleNode(new LMRAction("Middle"), new double[]{0.5});
        }
        for (int i = 0; i < 4; i++) {
            node.backUpSingleNode(new LMRAction("Right"), new double[]{0.4});
        }
        node.normaliseRewardsAfterIteration(new double[]{-1.0});
        node.normaliseRewardsAfterIteration(new double[]{0.5});

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

        for (int i = 0; i < 5; i++) {
            node.backUpSingleNode(new LMRAction("Middle"), new double[]{0.5});
        }
        for (int i = 0; i < 5; i++) {
            node.backUpSingleNode(new LMRAction("Right"), new double[]{0.4});
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

        for (int i = 0; i < 13; i++) {
            node.backUpSingleNode(new LMRAction("Middle"), new double[]{0.5});
        }
        for (int i = 0; i < 7; i++) {
            node.backUpSingleNode(new LMRAction("Right"), new double[]{0.4});
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
}
