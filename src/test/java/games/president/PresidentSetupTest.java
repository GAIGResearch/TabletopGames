package games.president;

import org.junit.Test;

import java.util.List;

import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * The first deal and the position it leaves.
 */
public class PresidentSetupTest {

    PresidentForwardModel fm = new PresidentForwardModel();

    private void assertDeal(int nPlayers, int... expectedSizes) {
        PresidentGameState state = newState(nPlayers, 3, fm);
        for (int p = 0; p < nPlayers; p++)
            assertEquals("hand size of player " + p, expectedSizes[p], state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(0, state.getSetSize());
        assertEquals(0, state.getPassesInRow());
        assertEquals(List.of(), state.getFinishingOrder());
        for (int p = 0; p < nPlayers; p++)
            assertEquals(0, state.getPlayerScore(p));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void theFirstDealStartsInThePlayPhaseWithNoExchange() {
        // even when playing to a target score with an exchange, the first deal has no President or Scum yet
        PresidentGameState state = newState(multiDealParams(11, 2), 4, 4, fm);
        assertEquals(PresidentGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(0, state.getCardsToGive());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void fourPlayersGetThirteenCardsEach() {
        // 52 = 4 x 13
        assertDeal(4, 13, 13, 13, 13);
    }

    @Test
    public void fivePlayersGetElevenOrTenCardsFromPlayerZero() {
        // 52 = 5 x 10 + 2: the two spare cards go to players 0 and 1
        assertDeal(5, 11, 11, 10, 10, 10);
    }

    @Test
    public void sevenPlayersGetEightOrSevenCardsFromPlayerZero() {
        // 52 = 7 x 7 + 3: the three spare cards go to players 0, 1 and 2
        assertDeal(7, 8, 8, 8, 7, 7, 7, 7);
    }

    @Test
    public void theDealDependsOnTheSeed() {
        // two states differing only in the seed: the shuffle gives player 0 different hands
        PresidentGameState a = newState(4, 1, fm);
        PresidentGameState b = newState(4, 2, fm);
        assertNotEquals(a.getPlayerHand(0).getComponents(), b.getPlayerHand(0).getComponents());
    }
}
