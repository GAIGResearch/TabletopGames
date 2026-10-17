package games.sueca;

import core.CoreConstants.GameResult;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of a single deal after the 10th trick, and each player's result.
 */
public class SuecaEndOfDealTest {

    @Test
    public void theLastTrickDecidesAWinForTeamOneAndBothPartnersWin() {
        SuecaGameState state = newState(8);
        SuecaForwardModel fm = new SuecaForwardModel();
        // team 0's 16 cards: K 4 x 4 = 16 + 7H 7D 7C 10 x 3 = 30 + JS JH JD 3 x 3 = 9, the rest 0: 55 card points
        arrangeLastTrick(state, 0,
                cards("KS", "KH", "KD", "KC", "7H", "7D", "7C", "JS", "JH", "JD",
                        "6S", "6H", "6D", "6C", "5S", "5H"),
                "2S", "AS", "3S", "4S");
        setTrumpCard(state, "2H");                     // Hearts trumps; the 2 of Hearts is on team 1's pile
        // team 1's 20 cards: AH AD AC 11 x 3 = 33 + 7S 10 + JC 3 + Q 2 x 4 = 8, the rest 0: 54
        assertEquals(20, state.getTeamPile(1).getSize());
        assertEquals(55, expectedCardPoints(cardsOf(state.getTeamPile(0))));
        assertEquals(54, expectedCardPoints(cardsOf(state.getTeamPile(1))));
        assertAllCardsPresent(state);

        playCards(state, fm, "2S", "AS", "3S");
        assertTrue("the deal ends only after the 40th card", state.isNotTerminal());
        playCards(state, fm, "4S");

        // player 1's Ace of Spades wins the last trick: team 1 54 + A 11 = 65, team 0 55 (55 + 65 = 120)
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(55, state.getCardPoints(0));
        assertEquals(65, state.getCardPoints(1));
        assertEquals(16, state.getTeamPile(0).getSize());
        assertEquals(24, state.getTeamPile(1).getSize());
        assertAllCardsPresent(state);
        // both partners win
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertEquals(55, state.getGameScore(0), 0.0);
        assertEquals(65, state.getGameScore(1), 0.0);
        assertEquals(55, state.getGameScore(2), 0.0);
        assertEquals(65, state.getGameScore(3), 0.0);
    }

    @Test
    public void sixtyAllIsADrawForAllFourPlayers() {
        SuecaGameState state = newState(8);
        SuecaForwardModel fm = new SuecaForwardModel();
        // team 0's 16 cards: A 11 x 4 = 44 + K 4 x 4 = 16, the 6s and 5s 0: 60
        arrangeLastTrick(state, 0,
                cards("AS", "AH", "AD", "AC", "KS", "KH", "KD", "KC",
                        "6S", "6H", "6D", "6C", "5S", "5H", "5D", "5C"),
                "2S", "3S", "4S", "2H");
        setTrumpCard(state, "2D");                     // Diamonds trumps; the 2 of Diamonds is on team 1's pile
        // team 1's 20 cards: 7 10 x 4 = 40 + J 3 x 4 = 12 + Q 2 x 4 = 8, the 4s, 3s and 2s 0: 60
        assertEquals(60, expectedCardPoints(cardsOf(state.getTeamPile(1))));

        // spades led; player 2's 4S wins a trick worth 0 (player 3's 2H is a discard): 60-60
        playCards(state, fm, "2S", "3S", "4S", "2H");
        assertFalse(state.isNotTerminal());
        assertEquals(60, state.getCardPoints(0));
        assertEquals(60, state.getCardPoints(1));
        assertEquals(20, state.getTeamPile(0).getSize());
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void aWinByOnePointForTeamZeroIsAWinForPlayersZeroAndTwo() {
        SuecaGameState state = newState(8);
        SuecaForwardModel fm = new SuecaForwardModel();
        // team 0's 16 cards as in the draw (60), the last trick led by player 3
        arrangeLastTrick(state, 3,
                cards("AS", "AH", "AD", "AC", "KS", "KH", "KD", "KC",
                        "6S", "6H", "6D", "6C", "5S", "5H", "5D", "5C"),
                "QS", "3S", "4S", "2S");
        setTrumpCard(state, "2D");
        // team 1 before: 60 - QS 2 = 58
        assertEquals(58, expectedCardPoints(cardsOf(state.getTeamPile(1))));

        // led by player 3 (2S), then 0 (QS), 1 (3S), 2 (4S): player 0's Queen wins Q 2: team 0 62, team 1 58
        playCards(state, fm, "2S", "QS", "3S", "4S");
        assertFalse(state.isNotTerminal());
        assertEquals(62, state.getCardPoints(0));
        assertEquals(58, state.getCardPoints(1));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }
}
