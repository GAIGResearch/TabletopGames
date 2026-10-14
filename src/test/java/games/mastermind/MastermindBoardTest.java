package games.mastermind;

import core.Game;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.grid.SquareCoord;
import games.GameType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MastermindBoardTest {

    Game game = GameType.Mastermind.createGameInstance(1, 40, new MMParameters());
    MMGameState state = (MMGameState) game.getGameState();
    MMForwardModel fm = new MMForwardModel();

    // guesses the colours (indices into MMConstants.guessColours) along the active row
    void guess(int... colours) {
        for (int c : colours)
            fm.next(state, new SetGridValueAction(state.getGuessBoard().getComponentID(), state.activeCol, state.activeRow,
                    MMConstants.guessColours.get(c).getComponentID()));
    }

    List<String> resultRow(int row) {
        List<String> retValue = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            BoardNode peg = state.getResultBoard().get(new SquareCoord(x, row));
            retValue.add(peg == null ? null : peg.getComponentName());
        }
        return retValue;
    }

    @Test
    public void aCompletedGuessGetsItsResults() {
        guess(0, 1, 2);
        assertEquals(MMConstants.guessColours.get(2), state.getGuessBoard().get(new SquareCoord(2, 0)));
        assertTrue(state.getResultBoard().isEmpty());
        guess(3);
        assertEquals(1, state.activeRow);
        assertEquals(0, state.activeCol);
        List<Integer> expected = state.checkGuessAgainstAnswer(state.getAnswerCode(), 0);
        List<String> names = new ArrayList<>();
        for (int r : expected)
            names.add(MMConstants.resultColours.get(r).getComponentName());
        assertEquals(names, resultRow(0));
    }

    @Test
    public void aPartGuessMatchingTheCodeSoFarDoesNotWin() {
        guess(MMConstants.guessColours.indexOf(state.getAnswerCode().get(0)));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void theHeuristicScoresTheResultPegs() {
        guess(0, 1, 2, 3);
        double expected = -3;
        for (int r : state.checkGuessAgainstAnswer(state.getAnswerCode(), 0))
            expected += r == 0 ? 2 : r == 1 ? 1 : 0;
        assertEquals(expected, state.getHeuristicScore(0), 1e-9);
    }

    @Test
    public void redeterminisedCodesAgreeWithTheLatestResults() {
        guess(0, 1, 2, 3);
        guess(4, 5, 0, 1);
        for (int i = 0; i < 20; i++) {
            MMGameState copy = (MMGameState) state.copy(0);
            for (int row = 0; row < 2; row++)
                assertEquals(state.checkGuessAgainstAnswer(state.getAnswerCode(), row),
                        copy.checkGuessAgainstAnswer(copy.getAnswerCode(), row));
        }
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        guess(0, 1);
        MMGameState copy = (MMGameState) state.copy();
        assertEquals(state, copy);
        fm.next(copy, fm.computeAvailableActions(copy).get(0));
        assertEquals(2, state.getGuessBoard().size());
        assertEquals(3, copy.getGuessBoard().size());
    }
}
