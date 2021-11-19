package players.mcts.test;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.components.Token;
import games.dominion.DominionGame;
import games.dominion.DominionParameters;
import games.tictactoe.*;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.SingleTreeNode;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TicTacToe {


    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    Token x = TicTacToeConstants.playerMapping.get(0);
    Token o = TicTacToeConstants.playerMapping.get(1);

    TicTacToeForwardModel fm = new TicTacToeForwardModel();

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 20;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.K = 1.0;
    }

    public Game createGame(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        return new TicTacToeGame(players, new TicTacToeGameParameters(3812));
    }

    @Test
    public void nodeCreatedIfGameOver() {
        Game ttt = createGame(params);
        TicTacToeGameState state = (TicTacToeGameState) ttt.getGameState();
        int board = state.getGridBoard().getComponentID();

        fm.next(state, new SetGridValueAction<>(board, 1, 0, x));
        fm.next(state, new SetGridValueAction<>(board, 0, 0, o));
        fm.next(state, new SetGridValueAction<>(board, 0, 2, x));
        fm.next(state, new SetGridValueAction<>(board, 1, 1, o));

        // o (p1) is now set to win on their next turn

        AbstractAction action = mctsPlayer.getAction(state, fm.computeAvailableActions(state));
        assertEquals(new SetGridValueAction<>(board, 2, 2, x), action);

        SingleTreeNode root = mctsPlayer.getRoot();
        assertEquals(200, root.getVisits());

    }

}
