package games.rummy;

import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Test;

import java.util.List;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal: hand sizes by player count, the discard pile, the draw deck, who starts.
 */
public class RummySetupTest {

    final RummyForwardModel fm = new RummyForwardModel();

    private void assertDeal(int nPlayers, int handSize) {
        RummyGameState state = newState(nPlayers, 3, fm);
        for (int p = 0; p < nPlayers; p++)
            assertEquals("hand of player " + p + " of " + nPlayers, handSize, state.getPlayerHand(p).getSize());
        assertEquals(1, state.getDiscardPile().getSize());
        // 52 cards less the hands less the one card that starts the discard pile
        assertEquals(52 - nPlayers * handSize - 1, state.getDrawDeck().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertNull(state.getTakenCard());
        assertTrue(state.getMelds().isEmpty());
        assertAllCardsPresent(state);
    }

    @Test
    public void twoPlayersAreDealtTenCardsEach() {
        assertDeal(2, 10);   // draw deck 52 - 20 - 1 = 31
    }

    @Test
    public void threeAndFourPlayersAreDealtSevenCardsEach() {
        assertDeal(3, 7);    // draw deck 52 - 21 - 1 = 30
        assertDeal(4, 7);    // draw deck 52 - 28 - 1 = 23
    }

    @Test
    public void fiveAndSixPlayersAreDealtSixCardsEach() {
        assertDeal(5, 6);    // draw deck 52 - 30 - 1 = 21
        assertDeal(6, 6);    // draw deck 52 - 36 - 1 = 15
    }

    @Test
    public void theHandSizeParameterSetsTheDeal() {
        RummyParameters params = RummyTestUtils.valetParams();
        params.setParameterValue("cardsFor2Players", 7);
        RummyGameState state = newState(params, 2, 3, fm);
        assertEquals(7, state.getPlayerHand(0).getSize());
        assertEquals(7, state.getPlayerHand(1).getSize());
        assertEquals(52 - 14 - 1, state.getDrawDeck().getSize());
    }

    @Test
    public void theDealIsShuffled() {
        // two deals differing only in the seed
        RummyGameState a = newState(2, 3, fm);
        RummyGameState b = newState(2, 4, fm);
        List<FrenchCard> handA = a.getPlayerHand(0).getComponents();
        List<FrenchCard> handB = b.getPlayerHand(0).getComponents();
        Deck<FrenchCard> drawA = a.getDrawDeck();
        Deck<FrenchCard> drawB = b.getDrawDeck();
        assertNotEquals(handA, handB);
        assertNotEquals(drawA.getComponents(), drawB.getComponents());
    }
}
