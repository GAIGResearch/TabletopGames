package players.mcts;

import core.AbstractPlayer;
import core.interfaces.IStateHeuristic;
import org.junit.Test;
import players.PlayerFactory;
import players.heuristics.AutomatedFeatures;
import players.heuristics.LeaderHeuristic;
import players.heuristics.LinearStateHeuristic;
import players.heuristics.LogisticActionHeuristic;
import players.simple.BoltzmannActionPlayer;
import utilities.JSONUtils;

import static evaluation.optimisation.TunableParameters.loadFromJSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static players.mcts.MCTSEnums.RolloutTermination.END_ROUND;
import static players.mcts.MCTSEnums.Strategies.PARAMS;
import static players.mcts.MCTSEnums.TreePolicy.UCB_Tuned;

public class LoadFromJSON {

    @Test
    public void loadFromJSONI() {
        AbstractPlayer agentOne = PlayerFactory.createPlayer("src/test/java/players/mcts/ValueNTBEA_00.json");

        MCTSParams params = (MCTSParams) agentOne.getParameters();
        assertEquals(3,params.rolloutLength);
        assertEquals(1, params.maxTreeDepth);
        assertEquals(1, params.getParameterValue("maxTreeDepth"));
        assertEquals(END_ROUND, params.rolloutTermination);
        assertEquals(PARAMS, params.rolloutType);
        assertEquals(UCB_Tuned, params.treePolicy);
        assertEquals(UCB_Tuned, params.getParameterValue("treePolicy"));

        IStateHeuristic heuristic = agentOne.parameters.getStateHeuristic();
        assertEquals(LinearStateHeuristic.class, heuristic.getClass());
        LinearStateHeuristic linearStateHeuristic = (LinearStateHeuristic) heuristic;
        // Leader
        assertEquals(new LeaderHeuristic(), linearStateHeuristic.getDefaultHeuristic());
        // AutomatedFeatures with 6 features
        assertEquals(6, linearStateHeuristic.names().length);
        assertEquals(7, linearStateHeuristic.coefficients().length);
        assertEquals(1, linearStateHeuristic.interactions().length);

        AbstractPlayer rolloutPolicy = params.getRolloutStrategy();
        assertEquals(BoltzmannActionPlayer.class, rolloutPolicy.getClass());
        BoltzmannActionPlayer boltzmannActionPlayer = (BoltzmannActionPlayer) rolloutPolicy;
        assertEquals(1.0, boltzmannActionPlayer.temperature, 0.0001);
        assertNull(params.getParameterValue("rolloutStrategy.temperature"));
        assertEquals(0.0, boltzmannActionPlayer.epsilon, 0.0001);
        assertNull(params.getParameterValue("rolloutStrategy.epsilon"));
    }

    @Test
    public void loadFromJSONII() {
        AbstractPlayer agentOne = PlayerFactory.createPlayer("src/test/java/players/mcts/ActionNTBEA_00.json");

        MCTSParams params = (MCTSParams) agentOne.getParameters();
        assertEquals(3,params.rolloutLength);
        assertEquals(1, params.maxTreeDepth);
        assertEquals(1, params.getParameterValue("maxTreeDepth"));
        assertEquals(END_ROUND, params.rolloutTermination);
        assertEquals(PARAMS, params.rolloutType);
        assertEquals(UCB_Tuned, params.treePolicy);
        assertEquals(UCB_Tuned, params.getParameterValue("treePolicy"));

        IStateHeuristic heuristic = agentOne.parameters.getStateHeuristic();
        assertEquals(LinearStateHeuristic.class, heuristic.getClass());
        LinearStateHeuristic linearStateHeuristic = (LinearStateHeuristic) heuristic;
        // Leader
        assertEquals(new LeaderHeuristic(), linearStateHeuristic.getDefaultHeuristic());
        // AutomatedFeatures with 6 features
        assertEquals(6, linearStateHeuristic.names().length);
        assertEquals(7, linearStateHeuristic.coefficients().length);
        assertEquals(1, linearStateHeuristic.interactions().length);

        LogisticActionHeuristic actionHeuristic = (LogisticActionHeuristic) params.actionHeuristic;
        assertEquals(23, actionHeuristic.names().length); // 2 + 21
        assertEquals(24, actionHeuristic.coefficients().length);
        assertEquals(1, actionHeuristic.interactions().length);

        AbstractPlayer rolloutPolicy = params.getRolloutStrategy();
        assertEquals(BoltzmannActionPlayer.class, rolloutPolicy.getClass());
        BoltzmannActionPlayer boltzmannActionPlayer = (BoltzmannActionPlayer) rolloutPolicy;
        assertEquals(0.1, boltzmannActionPlayer.temperature, 0.0001);
        assertEquals(0.3, boltzmannActionPlayer.epsilon, 0.0001);


        actionHeuristic = (LogisticActionHeuristic) boltzmannActionPlayer.getActionHeuristic();
        assertEquals(23, actionHeuristic.names().length); // 2 + 21
        assertEquals(24, actionHeuristic.coefficients().length);
        assertEquals(1, actionHeuristic.interactions().length);

    }
}
