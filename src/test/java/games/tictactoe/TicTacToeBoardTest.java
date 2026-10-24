package games.tictactoe;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.GameType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TicTacToeBoardTest {

    Game game = GameType.TicTacToe.createGameInstance(2, new TicTacToeGameParameters());
    TicTacToeGameState state = (TicTacToeGameState) game.getGameState();
    TicTacToeForwardModel fm = new TicTacToeForwardModel();

    // plays the squares in turn, x first
    void play(int... xy) {
        for (int i = 0; i < xy.length; i += 2) {
            int player = state.getCurrentPlayer();
            fm.next(state, new SetGridValueAction(state.getGridBoard().getComponentID(), xy[i], xy[i + 1],
                    TicTacToeConstants.playerMapping.get(player).getComponentID()));
        }
    }

    @Test
    public void actionsAreTheEmptySquaresColumnByColumn() {
        play(2, 0);
        List<String> squares = new ArrayList<>();
        for (AbstractAction a : fm.computeAvailableActions(state)) {
            SetGridValueAction s = (SetGridValueAction) a;
            assertEquals(TicTacToeConstants.playerMapping.get(1), s.getValue(state));
            squares.add(s.getX() + "," + s.getY());
        }
        assertEquals(List.of("0,0", "0,1", "0,2", "1,0", "1,1", "1,2", "2,1", "2,2"), squares);
        assertEquals(TicTacToeConstants.playerMapping.get(0), state.getGridBoard().getElement(2, 0));
    }

    @Test
    public void aColumnWins() {
        play(2, 0, 0, 0, 2, 1, 0, 1, 2, 2);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void aRowWins() {
        // o takes the middle row
        play(0, 0, 0, 1, 2, 2, 1, 1, 1, 0, 2, 1);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
    }

    @Test
    public void theSecondDiagonalWins() {
        play(2, 0, 0, 0, 1, 1, 1, 0, 0, 2);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
    }

    @Test
    public void aFullBoardWithNoLineIsADraw() {
        // x o x / x o o / o x x
        play(0, 0, 1, 0, 2, 0, 1, 1, 0, 1, 2, 1, 1, 2, 0, 2, 2, 2);
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        play(1, 1);
        TicTacToeGameState copy = (TicTacToeGameState) state.copy();
        assertEquals(state.getGridBoard(), copy.getGridBoard());
        fm.next(copy, fm.computeAvailableActions(copy).get(0));
        assertEquals(1, state.getGridBoard().size());
        assertEquals(2, copy.getGridBoard().size());
    }

    @Test
    public void theHeuristicCountsEmptySquaresForNoOne() {
        // x in the middle has four open lines; o has none
        play(1, 1);
        TicTacToeHeuristic h = new TicTacToeHeuristic();
        assertTrue(h.evaluateState(state, 0) > h.evaluateState(state, 1));
    }
    @Test
    public void aCopyKeepsTheBoardSize() {
        // set on the field, which a copy of the parameters does not keep
        TicTacToeGameParameters params = new TicTacToeGameParameters();
        params.gridSize = 4;
        Game big = GameType.TicTacToe.createGameInstance(2, params);
        TicTacToeGameState copy = (TicTacToeGameState) big.getGameState().copy();
        assertEquals(4, copy.getGridSize());
        assertEquals(16, fm.computeAvailableActions(copy).size());
    }
}
