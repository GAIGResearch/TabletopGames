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

    List<AbstractAction> baseActions = List.of(new LMRAction("Left"), new LMRAction("Middle"), new LMRAction("Right"));

    @Before
    public void setup() {
        params.firstPlayUrgency = 20000;
        fm.setup(game);
        player = new TestMCTSPlayer(params, STNWithTestInstrumentation::new);
        player.setForwardModel(fm);
    }

    @Test
    public void testFPU() {
        STNWithTestInstrumentation node = (STNWithTestInstrumentation) SingleTreeNode.createRootNode(player, game, rnd, STNWithTestInstrumentation::new);

        double[] results = node.actionValues(baseActions);
        assertEquals(3, results.length);
        assertEquals(20000, results[0], 0.001);
        assertEquals(20000, results[1], 0.001);
        assertEquals(20000, results[2], 0.001);

        node.backUpSingleNode(new LMRAction("Middle"), new double[]{1.0});
        node.normaliseRewardsAfterIteration(new double[]{1.0});
        double exploration = Math.sqrt(Math.log(2));
        results = node.actionValues(baseActions);
        assertEquals(3, results.length);
        assertEquals(20000, results[0], 0.001);
        // With only one result, this is normalised to 0.0
        assertEquals(exploration, results[1], 0.001);
        assertEquals(20000, results[2], 0.001);
    }

}
