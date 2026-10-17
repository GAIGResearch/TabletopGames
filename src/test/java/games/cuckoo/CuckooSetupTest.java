package games.cuckoo;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * The starting position: one card each, lives, dealer and first player.
 */
public class CuckooSetupTest {

    private static void assertStartingPosition(int nPlayers, int nLives) {
        CuckooGameState state = newState(nPlayers, nLives, 7);
        for (int p = 0; p < nPlayers; p++) {
            assertEquals("cards held by player " + p, 1, state.playerCards.get(p).getSize());
            assertEquals(VISIBLE_TO_OWNER, state.playerCards.get(p).getVisibilityMode());
            assertEquals(p, state.playerCards.get(p).getOwnerId());
            assertEquals("lives of player " + p, nLives, state.getLives(p));
            assertEquals("still in the game", -1, state.roundEliminated[p]);
        }
        assertEquals("draw deck holds 52 - n", 52 - nPlayers, state.drawDeck.getSize());
        assertAllCardsPresent(state);
        // the last player deals, so player 0 on the dealer's left decides first
        assertEquals(nPlayers - 1, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void everyPlayerStartsWithOneCardAndTheLastPlayerDeals() {
        assertStartingPosition(4, 3);
        assertStartingPosition(10, 3);
    }

    @Test
    public void everyPlayerStartsWithNLives() {
        assertStartingPosition(6, 1);
        assertStartingPosition(6, 5);
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        List<FrenchCard> deal1 = new ArrayList<>(), deal2 = new ArrayList<>();
        CuckooGameState s1 = newState(6, 3, 1), s2 = newState(6, 3, 2);
        for (int p = 0; p < 6; p++) {
            deal1.add(s1.getPlayerCard(p));
            deal2.add(s2.getPlayerCard(p));
        }
        assertNotEquals(deal1, deal2);
    }
}
