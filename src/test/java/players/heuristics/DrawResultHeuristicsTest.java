package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants.GameResult;
import core.interfaces.IStateHeuristic;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DrawAsWinHeuristic and DrawAsLossHeuristic score every result as WinOnlyHeuristic does, except a draw.
 */
public class DrawResultHeuristicsTest {

    private static double score(IStateHeuristic heuristic, GameResult result) {
        AbstractGameState gs = mock(AbstractGameState.class);
        when(gs.getPlayerResults()).thenReturn(new GameResult[]{result, LOSE_GAME});
        return heuristic.evaluateState(gs, 0);
    }

    @Test
    public void aDrawScoresAsAWinOrALoss() {
        assertEquals(0.5, score(new WinOnlyHeuristic(), DRAW_GAME), 0.0);
        assertEquals(1.0, score(new DrawAsWinHeuristic(), DRAW_GAME), 0.0);
        assertEquals(0.0, score(new DrawAsLossHeuristic(), DRAW_GAME), 0.0);
    }

    @Test
    public void everyOtherResultScoresAsWinOnly() {
        IStateHeuristic winOnly = new WinOnlyHeuristic();
        for (GameResult result : GameResult.values()) {
            if (result == DRAW_GAME) continue;
            assertEquals(result.name(), score(winOnly, result), score(new DrawAsWinHeuristic(), result), 0.0);
            assertEquals(result.name(), score(winOnly, result), score(new DrawAsLossHeuristic(), result), 0.0);
        }
    }

    @Test
    public void theRangeIsZeroToOne() {
        for (IStateHeuristic h : new IStateHeuristic[]{new DrawAsWinHeuristic(), new DrawAsLossHeuristic()}) {
            assertEquals(0.0, h.minValue(), 0.0);
            assertEquals(1.0, h.maxValue(), 0.0);
        }
    }
}
