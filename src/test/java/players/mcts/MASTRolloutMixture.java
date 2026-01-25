package players.mcts;

import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import org.junit.Before;
import org.junit.Test;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.*;

public class MASTRolloutMixture {

    MCTSParams params = new MCTSParams();
    TestMCTSPlayer player;

    LMRForwardModel fm = new LMRForwardModel();
    LMRGame game = new LMRGame(new LMTParameters(302));
    Random rnd = new Random(303897);
    STNWithTestInstrumentation node;

    List<AbstractAction> baseActions = List.of(new LMRAction("Left"), new LMRAction("Middle"), new LMRAction("Right"));

    @Before
    public void setup() {
        fm.setup(game);

    }

    private void initialiseMCTSPlayer() {
        // after setting params up in test
        params._reset();
        player = new TestMCTSPlayer(params, STNWithTestInstrumentation::new);
        player.setForwardModel(fm);
        node = (STNWithTestInstrumentation) SingleTreeNode.createRootNode(player, game, rnd, STNWithTestInstrumentation::new);
    }

    @Test
    public void classicMASTRolloutUpdatesStatisticsI() {
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.MAST);
        params.setParameterValue("MAST", MCTSEnums.MASTType.Both);
        params.setParameterValue("MASTBoltzmann", 0.1);
        initialiseMCTSPlayer();
        assertTrue(player.getParameters().getRolloutStrategy() instanceof MASTPlayer);
        assertTrue(player.getParameters().useMAST);
        MASTPlayer rolloutPlayer = (MASTPlayer) player.getParameters().rolloutPolicy;
        rolloutPlayer.setMASTStats(node.MASTStatistics); // link rollout player to MAST statistics

        node.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        // Create a singleton list with Left action
        List<Pair<Integer, AbstractAction>> actions = List.of(new Pair<>(0, new LMRAction("Left")));
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{1.0});
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{2.0});

        assertEquals(new Pair<>(2, 3.0), node.MASTStatistics.get(0).get(new LMRAction("Left")));
        assertNull(node.MASTStatistics.get(0).get(new LMRAction("Middle")));

        // Now check that the rollout policy uses the MAST statistics
        assertEquals(1.5, rolloutPlayer.valueOf(new LMRAction("Left"), game), 0.001);
        assertEquals(0.0, rolloutPlayer.valueOf(new LMRAction("Middle"), game), 0.001);
        assertEquals(0.0, rolloutPlayer.valueOf(new LMRAction("Right"), game), 0.001);

        // And check pdf values
        assertEquals(1.0, rolloutPlayer.probabilityOf(new LMRAction("Left"), game, baseActions), 0.001);
        assertEquals(0.0, rolloutPlayer.probabilityOf(new LMRAction("Middle"), game, baseActions), 0.001);
        assertEquals(0.0, rolloutPlayer.probabilityOf(new LMRAction("Right"), game, baseActions), 0.001);
    }


    @Test
    public void classicMASTRolloutUpdatesStatisticsII() {
        // as I, but with higher temperature and a new default value
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.MAST);
        params.setParameterValue("MAST", MCTSEnums.MASTType.Both);
        params.setParameterValue("MASTBoltzmann", 1.0);
        params.setParameterValue("MASTDefaultValue", 0.5);
        initialiseMCTSPlayer();
        assertTrue(player.getParameters().getRolloutStrategy() instanceof MASTPlayer);
        assertTrue(player.getParameters().useMAST);
        MASTPlayer rolloutPlayer = (MASTPlayer) player.getParameters().rolloutPolicy;
        rolloutPlayer.setMASTStats(node.MASTStatistics); // link rollout player to MAST statistics

        node.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        // Create a singleton list with Left action
        List<Pair<Integer, AbstractAction>> actions = List.of(new Pair<>(0, new LMRAction("Left")));
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{1.0});
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{2.0});

        assertEquals(new Pair<>(2, 3.0), node.MASTStatistics.get(0).get(new LMRAction("Left")));
        assertNull(node.MASTStatistics.get(0).get(new LMRAction("Middle")));

        // Now check that the rollout policy uses the MAST statistics
        assertEquals(1.5, rolloutPlayer.valueOf(new LMRAction("Left"), game), 0.001);
        assertEquals(0.5, rolloutPlayer.valueOf(new LMRAction("Middle"), game), 0.001);
        assertEquals(0.5, rolloutPlayer.valueOf(new LMRAction("Right"), game), 0.001);

        // And check pdf values
        assertEquals(0.576, rolloutPlayer.probabilityOf(new LMRAction("Left"), game, baseActions), 0.001);
        assertEquals(0.212, rolloutPlayer.probabilityOf(new LMRAction("Middle"), game, baseActions), 0.001);
        assertEquals(0.212, rolloutPlayer.probabilityOf(new LMRAction("Right"), game, baseActions), 0.001);
    }


    @Test
    public void mixtureRolloutPolicyUpdatesMASTStatistics() {
        // Here we set up a parameterised rollout policy that uses MAST statistics
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.PARAMS);
        params.setParameterValue("MAST", MCTSEnums.MASTType.Both);
        MASTPlayerParams mastParams = new MASTPlayerParams();
        mastParams.setParameterValue("defaultValue", 0.25);
        mastParams.setParameterValue("temperature", 0.6);
        mastParams.setParameterValue("externalHeuristic", (IActionHeuristic) (action, state, list) -> 47.0);
        mastParams.setParameterValue("weightOfExternal",  0.5);
        params.setParameterValue("rolloutPolicyParams", mastParams);
        initialiseMCTSPlayer();

        assertTrue(player.getParameters().getRolloutStrategy() instanceof MASTPlayer);

        MASTPlayer rolloutPlayer = (MASTPlayer) player.getParameters().rolloutPolicy;
        assertTrue(rolloutPlayer.getActionHeuristic() instanceof MASTPlusActionHeuristic);
        rolloutPlayer.setMASTStats(node.MASTStatistics); // link rollout player to MAST statistics

        node.backUpSingleNode(new LMRAction("Left"), new double[]{1.0});
        // Create a singleton list with Left action
        List<Pair<Integer, AbstractAction>> actions = List.of(new Pair<>(0, new LMRAction("Left")));
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{1.0});
        node.updateMASTStatistics(actions, new ArrayList<>(), new double[]{2.0});

        assertEquals(new Pair<>(2, 3.0), node.MASTStatistics.get(0).get(new LMRAction("Left")));
        assertNull(node.MASTStatistics.get(0).get(new LMRAction("Middle")));

        // Now check that the rollout policy uses the MAST statistics
        assertEquals((1.5 + 47.0) / 2.0, rolloutPlayer.valueOf(new LMRAction("Left"), game), 0.001);
        assertEquals((0.25 + 47.0) / 2.0, rolloutPlayer.valueOf(new LMRAction("Middle"), game), 0.001);
        assertEquals((0.25 + 47.0) / 2.0, rolloutPlayer.valueOf(new LMRAction("Right"), game), 0.001);

        // And check pdf values
        assertEquals(0.586, rolloutPlayer.probabilityOf(new LMRAction("Left"), game, baseActions), 0.001);
        assertEquals(0.207, rolloutPlayer.probabilityOf(new LMRAction("Middle"), game, baseActions), 0.001);
        assertEquals(0.207, rolloutPlayer.probabilityOf(new LMRAction("Right"), game, baseActions), 0.001);
    }
}
