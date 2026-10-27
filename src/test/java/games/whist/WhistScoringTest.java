package games.whist;

import core.CoreConstants.GameResult;
import core.Game;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * The last trick of a one-deal game, arranged and then played through fm.next: scoring the deal and ending the game.
 * Team 0 is players 0 and 2, team 1 players 1 and 3.
 */
public class WhistScoringTest {

    WhistGameState state;
    WhistForwardModel fm;

    @Before
    public void setup() {
        Game game = newGame(11);
        state = (WhistGameState) game.getGameState();
        fm = (WhistForwardModel) game.getForwardModel();
    }

    @Test
    public void nineTricksScoreThreePointsAndBothPartnersWin() {
        // before the last trick: team 0 4 + 4 = 8, team 1 2 + 2 = 4 (12 tricks played)
        arrangeLastTrick(state, 0, new int[]{4, 2, 4, 2}, "3H", "5H", "2S", "AH");
        setTrumps(state, "AS");
        playCards(state, fm, "3H", "5H", "2S", "AH");
        // the 2 of Spades trumps the Ace of Hearts: player 2 (team 0) wins the last trick

        assertArrayEquals(new int[]{4, 2, 5, 2}, state.tricksTaken);
        assertEquals(9, state.getTeamTricks(0));          // 4 + 5
        assertEquals(4, state.getTeamTricks(1));          // 2 + 2
        assertEquals(3, state.getTeamPoints(0));          // 9 - 6
        assertEquals(0, state.getTeamPoints(1));          // the side with fewer tricks scores nothing
        assertEquals(52, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());

        // one deal: the game ends. The partners share first place - the default endGame would call that a draw
        assertFalse(state.isNotTerminal());
        assertEquals(3.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(2), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(3), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void sevenTricksToSixScoresOnePointForTeamOne() {
        // before the last trick: 3 each, so 6 - 6. Player 1 leads; play goes 1, 2, 3, 0
        arrangeLastTrick(state, 1, new int[]{3, 3, 3, 3}, "10D", "KC", "QC", "AC");
        setTrumps(state, "4H");
        playCards(state, fm, "KC", "QC", "AC", "10D");
        // hearts trumps, none played: the Ace of Clubs (player 3) wins; player 0's 10D is off-suit

        assertArrayEquals(new int[]{3, 3, 3, 4}, state.tricksTaken);
        assertEquals(6, state.getTeamTricks(0));          // 3 + 3
        assertEquals(7, state.getTeamTricks(1));          // 3 + 4
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(1, state.getTeamPoints(1));          // 7 - 6

        assertFalse(state.isNotTerminal());
        assertEquals(1.0, state.getGameScore(1), 0.0);
        assertEquals(1.0, state.getGameScore(3), 0.0);
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void allThirteenTricksScoreSevenPoints() {
        // before the last trick: team 0 7 + 5 = 12, team 1 none. Player 2 leads; play goes 2, 3, 0, 1
        arrangeLastTrick(state, 2, new int[]{7, 0, 5, 0}, "3S", "4S", "AS", "2S");
        setTrumps(state, "9D");
        playCards(state, fm, "AS", "2S", "3S", "4S");
        // the Ace of Spades, led by player 2, is the highest spade

        assertArrayEquals(new int[]{7, 0, 6, 0}, state.tricksTaken);
        assertEquals(13, state.getTeamTricks(0));         // 7 + 6
        assertEquals(0, state.getTeamTricks(1));
        assertEquals(7, state.getTeamPoints(0));          // 13 - 6
        assertEquals(0, state.getTeamPoints(1));
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameDoesNotEndBeforeTheLastTrick() {
        // twelfth trick: 11 played (team 0 3 + 3 = 6, team 1 2 + 3 = 5), two cards each still to play
        arrangeLastTrick(state, 0, new int[]{3, 2, 3, 3}, "3H", "5H", "2S", "AH");
        giveHand(state, 0, "3H", "4D");
        giveHand(state, 1, "5H", "5D");
        giveHand(state, 2, "2S", "6D");
        giveHand(state, 3, "AH", "7D");
        setTrumps(state, "AS");
        playCards(state, fm, "3H", "5H", "2S", "AH");
        // player 2 trumps and wins trick 12; 6 + 1 = 7 to 5 so far, but a trick remains
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(2, state.getCurrentPlayer());
    }
}
