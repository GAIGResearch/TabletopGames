package games.gofish;

import org.junit.Test;

import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal: hand size by player count, the draw deck, books dealt, and the first player.
 */
public class GoFishSetupTest {

    /** Hand plus books of each player (a dealt book leaves the hand at once, so the two together are the deal). */
    private static void assertDealt(GoFishGameState state, int perPlayer) {
        int n = state.getNPlayers();
        for (int p = 0; p < n; p++)
            assertEquals("cards dealt to player " + p, perPlayer,
                    state.playerHands.get(p).getSize() + state.playerBooks.get(p).getSize());
        assertEquals("draw deck = 52 - " + n + " x " + perPlayer, 52 - n * perPlayer, state.drawDeck.getSize());
        assertAllCardsPresent(state);
        assertBooksWellFormed(state);
        assertEquals("player 0 starts", 0, state.getCurrentPlayer());
    }

    @Test
    public void fiveCardsEachWithThreeToSixPlayers() {
        for (int n = 3; n <= 6; n++) {
            GoFishGameState state = newState(n, 17);
            assertDealt(state, 5);   // e.g. 6 players: 52 - 6 x 5 = 22 in the draw deck
            assertEquals(5, ((GoFishParameters) state.getGameParameters()).handSize(n));
        }
    }

    @Test
    public void sevenCardsEachWithTwoPlayers() {
        GoFishGameState state = newState(2, 17);
        assertDealt(state, 7);       // 52 - 2 x 7 = 38 in the draw deck
        assertEquals(7, ((GoFishParameters) state.getGameParameters()).handSize(2));
    }

    @Test
    public void startingHandSizeParameterSetsTheDealForThreeOrMorePlayers() {
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("startingHandSize", 6);
        GoFishGameState state = newState(params, 4, 17);
        assertDealt(state, 6);       // 52 - 4 x 6 = 28
    }

    @Test
    public void twoPlayerHandSizeParameterSetsTheDealForTwoPlayers() {
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("twoPlayerHandSize", 4);
        GoFishGameState state = newState(params, 2, 17);
        assertDealt(state, 4);       // 52 - 2 x 4 = 44
    }

    @Test
    public void aBookInADealtHandIsLaidDownAtOnce() {
        // 17 cards each to 3 players makes dealt books common (about 1 deal in 4); every book must be laid down
        int handsWithABook = 0;
        for (long seed = 0; seed < 100; seed++) {
            GoFishParameters params = new GoFishParameters();
            params.setParameterValue("startingHandSize", 17);
            GoFishGameState state = newState(params, 3, seed);
            assertDealt(state, 17);  // 52 - 3 x 17 = 1; also checks no hand still holds 4 of a rank
            for (int p = 0; p < 3; p++)
                if (state.playerBooks.get(p).getSize() > 0) handsWithABook++;
        }
        assertTrue("no deal in 100 seeds produced a book", handsWithABook > 0);
    }
}
