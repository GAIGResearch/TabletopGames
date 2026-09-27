package games.schwimmen;

import core.CoreConstants;
import games.schwimmen.actions.Pass;
import org.junit.Test;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Hand values (the best single-suit total; three of a kind; three Aces), Schnauz / Feuer recognition, the parameters
 * behind them, and the result of the single deal. Tie-breaks between equal values are in SchwimmenTiebreakTest.
 */
public class SchwimmenScoringTest {

    SchwimmenParameters params = SchwimmenTestUtils.valetParams();

    private double value(String... codes) {
        return SchwimmenUtils.handValue(cards(codes), params);
    }

    @Test
    public void aHandIsWorthItsBestSingleSuitTotal() {
        // pagat's examples
        assertEquals(7 + 9, value("7H", "9H", "KS"), 1e-9);          // the two hearts, 16, beat the King's 10
        assertEquals(10, value("8C", "9D", "JS"), 1e-9);             // three suits: the Jack alone
        assertEquals(11 + 10 + 10, value("AH", "KH", "JH"), 1e-9);    // one suit: 31
        // more
        assertEquals(11 + 10, value("AS", "10S", "7D"), 1e-9);        // Ace 11 + Ten 10 in spades
        assertEquals(9, value("7C", "8D", "9H"), 1e-9);              // three suits, highest card a Nine
        assertEquals(9 + 8, value("QC", "9S", "8S"), 1e-9);          // two spades (17) beat the Queen (10)
        assertEquals(11, value("AD", "KC", "7S"), 1e-9);             // the Ace alone counts 11
        assertEquals(10 + 10 + 7, value("QD", "JD", "7D"), 1e-9);     // one suit: 27
    }

    @Test
    public void theGameScoreIsTheHandValue() {
        SchwimmenForwardModel fm = new SchwimmenForwardModel();
        SchwimmenGameState state = newState(3, ordinarySeed(3, 0), fm);
        arrangePlay(state, 0, h("AH", "7D", "10D"), h("7H", "9H", "KS"), h("8C", "9D", "JS"), h("QC", "9S", "8S"));
        assertEquals(7 + 9, state.getGameScore(0), 1e-9);
        assertEquals(10, state.getGameScore(1), 1e-9);
        assertEquals(9 + 8, state.getGameScore(2), 1e-9);
        assertEquals(7 + 9, state.getHandValue(0), 1e-9);
    }

    @Test
    public void theBestHandWinsWhenTheDealEnds() {
        SchwimmenForwardModel fm = new SchwimmenForwardModel();
        SchwimmenGameState state = newState(3, ordinarySeed(3, 0), fm);
        // player 0: QH 9H = 19; player 1: AS KS = 21; player 2 (dealer): 10C 8C = 18
        arrangePlay(state, 0, h("JD", "9S", "7D"), h("QH", "9H", "7C"), h("AS", "KS", "8D"), h("10C", "8C", "7S"));
        leaveInDrawDeck(state, 2);

        for (int i = 0; i < 3; i++)
            takeTurn(state, fm, new Pass());

        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void threeOfAKindIsWorthThirtyAndAHalfAndThreeAcesThirtyTwo() {
        assertEquals(30.5, value("KH", "KD", "KS"), 1e-9);          // three Kings (their best suit alone is 10)
        assertEquals(30.5, value("7C", "7D", "7H"), 1e-9);          // three Sevens (best suit alone is 7)
        assertEquals(32, value("AH", "AD", "AC"), 1e-9);            // Feuer (best suit alone is 11)
    }

    @Test
    public void pagatsRankingFeuerThenAceKingJackThenNinesThenKingQueenJack() {
        double feuer = value("AH", "AD", "AS");                      // 32
        double akj = value("AC", "KC", "JC");                        // 11 + 10 + 10 = 31
        double nines = value("9H", "9D", "9C");                      // 30.5
        double kqj = value("KS", "QS", "JS");                        // 10 + 10 + 10 = 30
        assertEquals(32, feuer, 1e-9);
        assertEquals(31, akj, 1e-9);
        assertEquals(30.5, nines, 1e-9);
        assertEquals(30, kqj, 1e-9);
        assertTrue(feuer > akj && akj > nines && nines > kqj);
    }

    @Test
    public void threeSevensBeatTwentyNineInOneSuit() {
        assertEquals(11 + 10 + 8, value("AH", "10H", "8H"), 1e-9);   // 29
        assertTrue(value("7C", "7D", "7S") > value("AH", "10H", "8H"));
    }

    @Test
    public void theThreeOfAKindValueComesFromTheParameters() {
        params.setParameterValue("threeOfAKindValue", 29.0);
        assertEquals(29, value("KH", "KD", "KS"), 1e-9);
        assertEquals(32, value("AH", "AD", "AC"), 1e-9);            // three Aces use their own parameter
        assertEquals(10 + 10 + 10, value("KS", "QS", "JS"), 1e-9);    // 30 beats three of a kind at 29
    }

    @Test
    public void theThreeAcesValueComesFromTheParameters() {
        params.setParameterValue("threeAcesValue", 35.0);
        assertEquals(35, value("AH", "AD", "AC"), 1e-9);
        assertEquals(30.5, value("KH", "KD", "KS"), 1e-9);
    }

    @Test
    public void schnauzIsThirtyOneInOneSuit() {
        assertTrue(SchwimmenUtils.isSchnauz(cards("AH", "KH", "JH"), params));    // 11 + 10 + 10
        assertTrue(SchwimmenUtils.isSchnauz(cards("10D", "AD", "QD"), params));   // any order
        assertFalse(SchwimmenUtils.isSchnauz(cards("AH", "KH", "9H"), params));   // 30
        assertFalse(SchwimmenUtils.isSchnauz(cards("AH", "KH", "JS"), params));   // 21 in hearts: 31 across two suits
        assertFalse(SchwimmenUtils.isSchnauz(cards("AH", "AD", "AC"), params));   // Feuer, not Schnauz
        assertFalse(SchwimmenUtils.isSchnauz(cards("KH", "KD", "KS"), params));   // three of a kind
    }

    @Test
    public void schnauzUsesTheSchnauzTotalParameter() {
        params.setParameterValue("schnauzTotal", 30);
        assertTrue(SchwimmenUtils.isSchnauz(cards("KS", "QS", "JS"), params));    // 10 + 10 + 10 = 30
        assertFalse(SchwimmenUtils.isSchnauz(cards("AH", "KH", "JH"), params));   // 31
    }

    @Test
    public void feuerIsThreeAces() {
        assertTrue(SchwimmenUtils.isFeuer(cards("AH", "AD", "AC")));
        assertTrue(SchwimmenUtils.isFeuer(cards("AS", "AC", "AD")));
        assertFalse(SchwimmenUtils.isFeuer(cards("KH", "KD", "KS")));             // three of a kind, not Aces
        assertFalse(SchwimmenUtils.isFeuer(cards("AH", "AD", "KC")));             // two Aces
        assertFalse(SchwimmenUtils.isFeuer(cards("AH", "KH", "JH")));             // Schnauz
    }

    @Test
    public void theGameScoreIsTheFullHandValue() {
        SchwimmenForwardModel fm = new SchwimmenForwardModel();
        SchwimmenGameState state = newState(3, ordinarySeed(3, 0), fm);
        arrangePlay(state, 0, h("7D", "8D", "9D"), h("QH", "QD", "QS"), h("AH", "AD", "AC"), h("AS", "KS", "JS"));
        assertEquals(30.5, state.getGameScore(0), 1e-9);
        assertEquals(32, state.getGameScore(1), 1e-9);
        assertEquals(11 + 10 + 10, state.getGameScore(2), 1e-9);
    }
}
