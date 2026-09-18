package games.blackjack;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

import static games.blackjack.BlackjackGameState.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

public class BlackjackSetupTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 3);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    @Test
    public void noCardsAreDealtBeforeTheBetsAndPlayerZeroBetsFirst() {
        assertEquals(52, state.getDrawDeck().getSize());
        assertEquals(new HashSet<>(FULL_DECK), setOf(state.getDrawDeck()));
        assertEquals(0, state.getDealerHand().getSize());
        assertEquals(0, state.getHoleCard().getSize());
        for (int p = 0; p < 3; p++) {
            assertEquals(10, state.getChips(p));
            assertEquals(1, state.getPlayerHands(p).size());
            assertEquals(0, state.getPlayerHand(p, 0).getSize());
            assertEquals(0, state.getBet(p, 0));
        }
        assertEquals(Betting, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void everyPlayerStartsWithStartingChips() {
        params.setParameterValue("startingChips", 20);
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        for (int p = 0; p < 3; p++)
            assertEquals(20, state.getChips(p));
    }

    @Test
    public void cardValuesCountCourtCardsTenAndAcesOne() {
        assertEquals(10, cardValue(card("JH")));
        assertEquals(10, cardValue(card("QD")));
        assertEquals(10, cardValue(card("KS")));
        assertEquals(10, cardValue(card("10C")));
        assertEquals(2, cardValue(card("2C")));
        assertEquals(1, cardValue(card("AS")));
    }

    @Test
    public void handValueCountsOneAceAsElevenWhenItFits() {
        assertEquals(20, handValue(cards("KS QH")));
        assertEquals(17, handValue(cards("AS 6H")));         // soft 17
        assertTrue(isSoft(cards("AS 6H")));
        assertEquals(17, handValue(cards("AS 6H 10D")));     // the Ace drops to 1: hard 17
        assertFalse(isSoft(cards("AS 6H 10D")));
        assertEquals(21, handValue(cards("AS AH 9D")));      // 11 + 1 + 9
        assertTrue(isSoft(cards("AS AH 9D")));
        assertEquals(12, handValue(cards("AS AH")));         // 11 + 1
        assertTrue(isSoft(cards("AS AH")));
        assertEquals(21, handValue(cards("AS KH")));
        assertEquals(26, handValue(cards("10S 6H KD")));     // bust: over 21
        assertFalse(isSoft(cards("10S 7H")));
    }

    @Test
    public void theHeuristicIsChipsOverTheMostAPlayerCouldHold() {
        // defaults: 10 + 1 hand x (stake 10 at 2:1 = 20, plus insurance 10) = 40, so 10 chips score 0.25
        assertEquals(40, params.maxChips());
        assertEquals(0.25, state.getHeuristicScore(0), 1e-9);

        // stake 10 x 4 split hands = 40, at 1.5 = 60, plus insurance 10: 70 a hand, x 3 hands + 10 = 220
        params.setParameterValue("splitting", true);
        params.setParameterValue("doubleDown", true);
        params.setParameterValue("nHands", 3);
        params.setParameterValue("payout21", 1.5);
        assertEquals(220, params.maxChips());
    }
}
