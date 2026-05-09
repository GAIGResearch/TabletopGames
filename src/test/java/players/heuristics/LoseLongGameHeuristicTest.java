package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LoseLongGameHeuristicTest {

    private final LoseLongGameHeuristic heuristic = new LoseLongGameHeuristic();
    private final int playerId = 0;
    private final int minGameLength = 50;

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private AbstractGameState setupMockState(int gameTick, double player0Score, double player1Score, CoreConstants.GameResult p0Result, CoreConstants.GameResult p1Result) {
        AbstractGameState gs = mock(AbstractGameState.class);
        when(gs.getGameTick()).thenReturn(gameTick);
        when(gs.getGameScore(0)).thenReturn(player0Score);
        when(gs.getGameScore(1)).thenReturn(player1Score);
        when(gs.getNPlayers()).thenReturn(2);
        when(gs.getPlayerResults()).thenReturn(new CoreConstants.GameResult[]{p0Result, p1Result});
        return gs;
    }

    private double calculateExpected(int gameTick, double player0Score, double player1Score, boolean isWin) {
        double sigmoidScale = 0.2;
        double weightForGameLength = 0.5;
        double lengthComponent = sigmoid((double) (gameTick - minGameLength) / (sigmoidScale * minGameLength)) * weightForGameLength;
        double lossDiff = player1Score - player0Score;
        int targetLossDiff = 5;
        double scoreComponent = sigmoid((targetLossDiff - lossDiff) / (sigmoidScale * targetLossDiff));
        scoreComponent = (1.0 - Math.abs(scoreComponent - 0.5) * 2) * (1 - weightForGameLength);
        double winPenalty = 0.5;
        double winComponent = isWin ? winPenalty : 0;
        return Math.max(0, lengthComponent + scoreComponent - winComponent);
    }


    @Test
    public void gameInProgress_MinLengthGame_ExactDiff() {
        // We have reached the min length (so 0.5 for the length) and are exactly at the target loss diff (so 1.0 for the score).
        AbstractGameState gs = setupMockState(minGameLength, 5.0, 10.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = 0.75;
        System.out.println("Expected score for min length and exact diff: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void gameInProgress_MinLengthGame_ABitOff() {
        // We have reached the min length (so 0.5 for the length) and are exactly at the target loss diff (so 1.0 for the score).
        AbstractGameState gs = setupMockState(minGameLength, 5.0, 6.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = calculateExpected(minGameLength, 5.0, 6.0, false);
        System.out.println("Expected score for min length and exact diff: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
        expected = calculateExpected(minGameLength, 5.0, 4.0, false);
        gs = setupMockState(minGameLength, 5.0, 4.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void gameInProgress_LongGame_EqualScores() {
        // 1) Game is in progress, and the game length is 10 moves over the minimum. Scores are Equal.
        int gameTick = minGameLength + 10;
        AbstractGameState gs = setupMockState(gameTick, 10.0, 10.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = calculateExpected(gameTick, 10.0, 10.0, false);
        System.out.println("Expected +10 moves, equal scores: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void gameInProgress_LongGame_WinningScore() {
        // 2) As above, but Score is 5 points above opponent (i.e. winning)
        int gameTick = minGameLength + 10;
        AbstractGameState gs = setupMockState(gameTick, 15.0, 10.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = calculateExpected(gameTick, 15.0, 10.0, false);
        System.out.println("Expected score for +10 moves, winning score +5: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void gameInProgress_LongGame_LosingScore() {
        // 3) As above, but Score is 3 points below opponent (i.e. losing)
        int gameTick = minGameLength + 10;
        AbstractGameState gs = setupMockState(gameTick, 5.0, 10.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = calculateExpected(gameTick, 5.0, 10.0, false);
        System.out.println("Expected score for +10 moves, losing score -5: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void gameInProgress_ShortGame_EqualScores() {
        // 4) As above, but game length is 5 moves below minimum length.
        int gameTick = minGameLength - 5;
        AbstractGameState gs = setupMockState(gameTick, 10.0, 10.0, CoreConstants.GameResult.GAME_ONGOING, CoreConstants.GameResult.GAME_ONGOING);
        double expected = calculateExpected(gameTick, 10.0, 10.0, false);
        System.out.println("Expected score for short game (-5) with equal scores: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void terminalWin_LongGame_WinningScore() {
        // 5) Repeat 2 (Score 5 above opponent, 10 over min length), but with the game being Terminal (WIN).
        int gameTick = minGameLength + 10;
        AbstractGameState gs = setupMockState(gameTick, 15.0, 10.0, CoreConstants.GameResult.WIN_GAME, CoreConstants.GameResult.LOSE_GAME);
        double expected = calculateExpected(gameTick, 15.0, 10.0, true);
        System.out.println("Expected score for terminal win (+10) with winning score (+5): " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void terminalLoss_LongGame_LosingScore() {
        // 5) Repeat 2 (Score 5 above opponent, 10 over min length), but with the game being Terminal (WIN).
        int gameTick = minGameLength + 10;
        AbstractGameState gs = setupMockState(gameTick, 0.0, 10.0, CoreConstants.GameResult.LOSE_GAME, CoreConstants.GameResult.LOSE_GAME);
        double expected = calculateExpected(gameTick, 0.0, 10.0, false);
        System.out.println("Expected score for terminal loss (+10 moves) with losing score (-10): " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }

    @Test
    public void terminalLoss_ShortGame_EqualScores() {
        // 6) Repeat 4 (Scores equal, 5 below min length), but with the game being Terminal (LOSE).
        int gameTick = minGameLength - 5;
        AbstractGameState gs = setupMockState(gameTick, 10.0, 10.0, CoreConstants.GameResult.LOSE_GAME, CoreConstants.GameResult.WIN_GAME);
        double expected = calculateExpected(gameTick, 10.0, 10.0, false);
        System.out.println("Expected score for terminal loss with equal scores: " + expected);
        assertEquals(expected, heuristic.evaluateState(gs, playerId), 0.0001);
    }
}
