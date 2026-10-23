package games.connect4;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.GameType;
import org.junit.Test;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The board is 7 wide and 6 high, with y = 0 at the top: a token dropped in an empty column lands at y = 5.
 */
public class Connect4BoardTest {

    Connect4ForwardModel fm = new Connect4ForwardModel();

    Connect4GameState newGame(Connect4GameParameters params) {
        Connect4GameState state = (Connect4GameState) GameType.Connect4.createGameState(params, 2);
        fm.setup(state);
        return state;
    }

    // drops a token in each column in turn, x first
    void play(Connect4GameState state, int... columns) {
        for (int col : columns) {
            SetGridValueAction chosen = null;
            for (AbstractAction a : fm.computeAvailableActions(state))
                if (((SetGridValueAction) a).getX() == col)
                    chosen = (SetGridValueAction) a;
            assertNotNull("column " + col + " is full", chosen);
            fm.next(state, chosen);
        }
    }

    @Test
    public void tokensDropToTheLowestEmptySquare() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 0, 3, 3, 3, 3, 3, 3);
        List<String> squares = new ArrayList<>();
        for (AbstractAction a : fm.computeAvailableActions(state))
            squares.add(((SetGridValueAction) a).getX() + "," + ((SetGridValueAction) a).getY());
        // column 3 is full
        assertEquals(List.of("0,4", "1,5", "2,5", "4,5", "5,5", "6,5"), squares);
        assertEquals(Connect4Constants.playerMapping.get(0), state.getGridBoard().getElement(0, 5));
        assertEquals(Connect4Constants.playerMapping.get(1), state.getGridBoard().getElement(3, 5));
        assertEquals(Connect4Constants.playerMapping.get(0), state.getGridBoard().getElement(3, 0));
    }

    @Test
    public void fourInAColumnWins() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 0, 1, 0, 1, 0, 1, 0);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(List.of(new Pair<>(0, 5), new Pair<>(0, 4), new Pair<>(0, 3), new Pair<>(0, 2)), state.getWinningCells());
    }

    @Test
    public void fourInARowWins() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 0, 0, 1, 1, 2, 2, 3);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(List.of(new Pair<>(0, 5), new Pair<>(1, 5), new Pair<>(2, 5), new Pair<>(3, 5)), state.getWinningCells());
    }

    @Test
    public void fourUpToTheRightWins() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 0, 1, 1, 2, 3, 2, 2, 3, 6, 3, 3);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(List.of(new Pair<>(0, 5), new Pair<>(1, 4), new Pair<>(2, 3), new Pair<>(3, 2)), state.getWinningCells());
    }

    @Test
    public void fourUpToTheLeftWins() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 6, 5, 5, 4, 3, 4, 4, 3, 0, 3, 3);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(4, state.getWinningCells().size());
        assertTrue(state.getWinningCells().contains(new Pair<>(3, 2)));
        assertTrue(state.getWinningCells().contains(new Pair<>(6, 5)));
    }

    @Test
    public void fourUpToTheLeftAboveTheBottomWins() {
        // the line from (6, 4), one above the bottom of the last column
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 5, 3, 3, 4, 4, 5, 5, 6, 6, 3, 3, 4, 4, 0, 3);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(List.of(new Pair<>(6, 4), new Pair<>(5, 3), new Pair<>(4, 2), new Pair<>(3, 1)), state.getWinningCells());
    }

    @Test
    public void aFullBoardIsADraw() {
        // no line of six fits on a board five wide and four high
        Connect4GameParameters params = new Connect4GameParameters();
        params.setParameterValue("width", 5);
        params.setParameterValue("height", 4);
        params.setParameterValue("winCount", 6);
        Connect4GameState state = newGame(params);
        for (int i = 0; i < 4; i++)
            play(state, 0, 1, 2, 3, 4);
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        Connect4GameState state = newGame(new Connect4GameParameters());
        play(state, 2);
        Connect4GameState copy = (Connect4GameState) state.copy();
        assertEquals(state, copy);
        play(copy, 2);
        assertEquals(1, state.getGridBoard().size());
        assertEquals(2, copy.getGridBoard().size());
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopyKeepsTheBoardSize() {
        // set on the fields, which a copy of the parameters loses (it keeps only the tunable values)
        Connect4GameParameters params = new Connect4GameParameters();
        params.width = 5;
        params.height = 4;
        Connect4GameState copy = (Connect4GameState) newGame(params).copy();
        assertEquals(5, copy.getWidth());
        assertEquals(4, copy.getHeight());
        assertEquals(List.of(3, 3, 3, 3, 3), fm.computeAvailableActions(copy).stream()
                .map(a -> ((SetGridValueAction) a).getY()).toList());
    }
}
