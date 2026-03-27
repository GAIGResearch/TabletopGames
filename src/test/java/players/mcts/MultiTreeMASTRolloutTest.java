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
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 500;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.rolloutType = MCTSEnums.Strategies.MAST;
        params.useMAST = true;
        params.MAST = MCTSEnums.MASTType.Both;
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
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;

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

    private MASTPlayer getMockRollout() {
        MASTPlayer mockRollout = Mockito.mock(MASTPlayer.class);
        when(mockRollout.getAction(any(), any())).thenAnswer(invocation -> {
            List<AbstractAction> actions = invocation.getArgument(1);
            return actions.getFirst();
        });
        when(mockRollout.getParameters()).thenReturn(params);
        return mockRollout;
    }
    @Test
    public void checkRollOutPolicyIsCalledInMultiTreeDefault() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        params.oppModelType = MCTSEnums.Strategies.DEFAULT; // Rollout used for opponent too

        MASTPlayer mockRollout = getMockRollout();
        params.setRolloutPolicy(mockRollout);

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        assertEquals(0, state.getCurrentPlayer());
        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        SingleTreeNode root = ((MultiTreeNode) mctsPlayer.root).getRoot(0);
        assertEquals(500, root.getVisits());
        assertEquals(501, mctsPlayer.root.copyCount);
        assertTrue(mctsPlayer.root.fmCallsCount > 5000); // rollout of 10 x 500 as minimum

        // we should use the rollout policy for most of these
        verify(mockRollout, times(5000)).getAction(any(), any());
    }

    @Test
    public void checkRollOutPolicyIsCalledInSingleTreeDefault() {
        params.oppModelType = MCTSEnums.Strategies.DEFAULT;

        MASTPlayer mockRollout = getMockRollout();
        params.setRolloutPolicy(mockRollout);

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        assertEquals(0, state.getCurrentPlayer());
        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        assertEquals(500, mctsPlayer.root.getVisits());
        assertEquals(501, mctsPlayer.root.copyCount);
        assertTrue(mctsPlayer.root.fmCallsCount > 5000); // rollout of 10 x 500 as minimum

        // we should use the rollout policy for most of these
        verify(mockRollout, times(5000)).getAction(any(), any());
    }

    @Test
    public void checkRollOutPolicyIsCalledInSingleTreeRandom() {
        params.oppModelType = MCTSEnums.Strategies.RANDOM;
        MASTPlayer mockRollout = getMockRollout();
        params.setRolloutPolicy(mockRollout);

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        assertEquals(0, state.getCurrentPlayer());
        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        assertEquals(500, mctsPlayer.root.getVisits());
        assertEquals(501, mctsPlayer.root.copyCount);
        assertTrue(mctsPlayer.root.fmCallsCount > 5000); // rollout of 10 x 1000 as minimum

        // we should use the rollout policy for most of these
        verify(mockRollout, atLeast(4000 / 3)).getAction(any(), any());
        verify(mockRollout, atMost(6000 / 3)).getAction(any(), any());
    }

    @Test
    public void checkRollOutPolicyIsCalledLessWithMultiTreeRNDOpponentModel() {
        params.oppModelType = MCTSEnums.Strategies.RANDOM;

        MASTPlayer mockRollout = getMockRollout();
        params.setRolloutPolicy(mockRollout);

        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        assertEquals(0, state.getCurrentPlayer());
        game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        assertEquals(500, mctsPlayer.root.getVisits());
        assertEquals(501, mctsPlayer.root.copyCount);
        assertTrue(mctsPlayer.root.fmCallsCount > 5000); // rollout of 10 x 1000 as minimum

        // we should use the rollout policy for most of these
        verify(mockRollout, atLeast(4000 / 3)).getAction(any(), any());
        verify(mockRollout, atMost(6000 / 3)).getAction(any(), any());
    }

}

