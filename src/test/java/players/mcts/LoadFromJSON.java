package players.mcts;

import core.AbstractPlayer;
import core.interfaces.IStateHeuristic;
import org.junit.Test;
import players.PlayerFactory;
import players.heuristics.LinearStateHeuristic;
import players.simple.BoltzmannActionPlayer;
import utilities.JSONUtils;

import static evaluation.optimisation.TunableParameters.loadFromJSON;
import static org.junit.Assert.assertEquals;
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

        AbstractPlayer rolloutPolicy = params.getRolloutStrategy();
        assertEquals(BoltzmannActionPlayer.class, rolloutPolicy);
    }
}
