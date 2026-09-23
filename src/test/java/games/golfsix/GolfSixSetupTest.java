package games.golfsix;

import org.junit.Test;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal made by _setup.
 */
public class GolfSixSetupTest {

    @Test
    public void eachPlayerHasSixFaceDownCardsAndOneCardStartsTheDiscardPile() {
        for (int n = 2; n <= 4; n++) {
            GolfSixGameState state = newState(n, 11);
            for (int p = 0; p < n; p++) {
                assertEquals("grid size, player " + p, 6, state.getGrid(p).getSize());
                assertEquals("all face-down, player " + p, "DDDDDD", faceUpPattern(state, p));
                assertEquals(0, state.getScore(p));
            }
            assertEquals(1, state.getDiscardPile().getSize());
            assertEquals(VISIBLE_TO_ALL, state.getDiscardPile().getVisibilityMode());
            // 52 cards - 6 per player - 1 discard
            assertEquals(52 - 6 * n - 1, state.getDrawDeck().getSize());
            assertNull(state.getDrawnCard());
            assertEquals(-1, state.getFinisher());
            assertEquals("the last player deals", n - 1, state.getDealer());
            assertEquals("player 0 plays first", 0, state.getCurrentPlayer());
            assertAllCardsPresent(state);
        }
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        GolfSixGameState a = newState(3, 1), b = newState(3, 2);
        assertNotEquals(a.getGrid(0).getComponents(), b.getGrid(0).getComponents());
        // and the same seed gives the same deal
        assertEquals(a.getGrid(0).getComponents(), newState(3, 1).getGrid(0).getComponents());
    }
}
