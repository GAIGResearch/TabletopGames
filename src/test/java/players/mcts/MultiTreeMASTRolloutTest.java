package players.mcts;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import games.loveletter.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiTreeMASTRolloutTest {

    MCTSPlayer mctsPlayer;
    MCTSParams params;

    AbstractForwardModel fm;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams();
        params.setRandomSeed(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 20;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.maintainMasterState = true;
        params.K = 1.0;
    }

    public Game createLoveLetter(MCTSParams params) {
        mctsPlayer = new MCTSPlayer(params);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(3024)));
        LoveLetterParameters gameParams = new LoveLetterParameters();
        gameParams.setRandomSeed(3812);
        Game game = GameType.LoveLetter.createGameInstance(players.size(), gameParams);
        game.reset(players);
        return game;
    }

    @Test
    public void MASTRolloutInLoveLetter() {
        params.budget = 1000;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        params.rolloutType = MCTSEnums.Strategies.MAST;
        params.useMAST = true;
        params.MAST = MCTSEnums.MASTType.Both;

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        // we should be able to get the rollout policy from params, and check it has MAST Statistics
        AbstractPlayer rollout = mctsPlayer.getParameters().getRolloutStrategy();
        assertTrue(rollout instanceof MASTPlayer);
        MASTPlayer mastRollout = (MASTPlayer) rollout;

        var MASTStats = mastRollout.getMASTStats();
        assertEquals(0, MASTStats.size());

        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        MASTStats = mastRollout.getMASTStats();
        assertEquals(3, MASTStats.size());
        for (int p = 0; p < 3; p++) {
            assertFalse(MASTStats.get(p).isEmpty());
        }
    }

    @Test
    public void checkRollOutPolicyIsCalled() {
        params.budget = 1000;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        params.rolloutType = MCTSEnums.Strategies.MAST;
        params.useMAST = true;
        params.MAST = MCTSEnums.MASTType.Both;

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        MASTPlayer mockPlayer = Mockito.mock(MASTPlayer.class);
        when(mockPlayer.getAction(any(), any())).thenAnswer(invocation -> {
            List<AbstractAction> actions = invocation.getArgument(1);
            return actions.getFirst();
        });
        params.setRolloutPolicy(mockPlayer);

        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        verify(mockPlayer, atLeast(1000)).getAction(any(), any());
        verify(mockPlayer, atMost(20000)).getAction(any(), any());
    }

}

