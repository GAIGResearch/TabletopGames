package games.skitgubbe;

import org.junit.Test;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

public class SkitgubbeSetupTest {

    private void checkSetup(int nPlayers, int handSize) {
        SkitgubbeParameters params = new SkitgubbeParameters();
        params.setParameterValue("handSize", handSize);
        SkitgubbeGameState state = newState(nPlayers, 42, params);

        for (int p = 0; p < nPlayers; p++) {
            assertEquals("hand of " + p, handSize, state.getPlayerHand(p).getSize());
            assertEquals(0, state.getCollectedCards(p).getSize());
            assertEquals(0, state.getHeldCards(p).getSize());
            assertEquals(0, state.getExitScore(p));
        }
        assertEquals(52 - handSize * nPlayers, state.getDrawDeck().getSize());
        assertEquals(0, state.getTrick().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(0, state.getTrumpCard().getSize());
        assertEquals(-1, state.getTrumpCard().getOwnerId());
        assertNull(state.getTrumpSuit());
        assertEquals(-1, state.getTrumpPlayer());
        assertEquals(SkitgubbeGameState.Phase.PHASE_ONE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void threePlayersAreDealtThreeCardsEachAndTheRestFormTheDrawDeck() {
        // 52 - 3 * 3 = 43 in the draw deck
        checkSetup(3, 3);
    }

    @Test
    public void fourPlayersAreDealtThreeCardsEachAndTheRestFormTheDrawDeck() {
        // 52 - 3 * 4 = 40 in the draw deck
        checkSetup(4, 3);
    }

    @Test
    public void handSizeTwoDealsTwoCardsEach() {
        // 52 - 2 * 3 = 46 in the draw deck
        checkSetup(3, 2);
    }
}
