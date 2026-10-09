package games.schwimmen;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import games.schwimmen.actions.ChooseHand;
import games.schwimmen.actions.ExchangeAll;
import games.schwimmen.actions.ExchangeOne;
import games.schwimmen.actions.Pass;
import org.junit.Test;

import java.util.HashSet;

import static core.CoreConstants.GameResult.*;
import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Schnauz (31 in one suit) or Feuer (three Aces) in any player's hand ends the deal (the game) at once and every hand
 * is scored: after the deal (before the dealer's choice), after the dealer's choice, and after an exchange (with no
 * close decision), in the last round after a close too. The extra hand is not a player's hand. Three of a kind that
 * is not Aces is only a hand value. 3 players (dealer 2) unless stated.
 */
public class SchwimmenSpecialHandsTest {

    SchwimmenForwardModel fm = new SchwimmenForwardModel();

    private SchwimmenGameState state3() {
        return newState(3, ordinarySeed(3, 0), fm);
    }

    private void assertEndedWith(SchwimmenGameState state, GameResult... results) {
        assertFalse("the deal should have ended", state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(results, state.getPlayerResults());
    }

    // ---- after the deal ----

    @Test
    public void aSchnauzDealtToAPlayerEndsTheDealBeforeTheDealersChoice() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("7C", "8C", "9C"),
                h("7H", "8D", "9S"), h("AH", "KH", "JH"), h("10S", "QD", "8H"));
        assertTrue(fm.endDealIfSpecialHand(state));
        // player 1: 11 + 10 + 10 = 31; player 0: 9 (three suits); player 2: 10 (three suits)
        assertEndedWith(state, LOSE_GAME, WIN_GAME, LOSE_GAME);
        assertEquals(3, state.getExtraHand().getSize());   // the dealer never chose
    }

    @Test
    public void feuerDealtBeatsASchnauzDealtAtTheSameTime() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("7C", "8C", "9C"),
                h("AS", "KS", "10S"), h("AH", "AD", "AC"), h("7H", "8D", "9S"));
        assertTrue(fm.endDealIfSpecialHand(state));
        // Feuer 32 > Schnauz 31
        assertEndedWith(state, LOSE_GAME, WIN_GAME, LOSE_GAME);
    }

    @Test
    public void withThreeAcesValuedBelowThirtyOneTheSchnauzWins() {
        SchwimmenParameters params = SchwimmenTestUtils.valetParams();
        params.setParameterValue("threeAcesValue", 30.0);
        SchwimmenGameState state = newState(params, 3, ordinarySeed(3, 0), fm);
        arrangeDeal(state, h("7C", "8C", "9C"),
                h("AS", "KS", "10S"), h("AH", "AD", "AC"), h("7H", "8D", "9S"));
        assertTrue(fm.endDealIfSpecialHand(state));
        // Feuer still ends the deal, but is worth only 30 < 31
        assertEndedWith(state, WIN_GAME, LOSE_GAME, LOSE_GAME);
    }

    @Test
    public void twoSchnauzDealtTogetherTheHigherSuitWins() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("7C", "8C", "9C"),
                h("AD", "KD", "QD"), h("7H", "8D", "9S"), h("AC", "KC", "10C"));
        assertTrue(fm.endDealIfSpecialHand(state));
        // 31 in Diamonds vs 31 in Clubs: Clubs
        assertEndedWith(state, LOSE_GAME, LOSE_GAME, WIN_GAME);
    }

    @Test
    public void threeKingsDealtDoNotEndTheDeal() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("7C", "8C", "9C"),
                h("KH", "KD", "KS"), h("AH", "10H", "9D"), h("QS", "JS", "7D"));
        assertFalse(fm.endDealIfSpecialHand(state));
        assertTrue(state.isNotTerminal());
        assertTrue(state.isDealerChoicePending());
        assertEquals(30.5, state.getHandValue(0), 1e-9);
    }

    @Test
    public void aSchnauzInTheExtraHandDoesNotEndTheDeal() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("AC", "KC", "QC"),
                h("7H", "8D", "9S"), h("AH", "10H", "9D"), h("QS", "JS", "7D"));
        assertFalse(fm.endDealIfSpecialHand(state));
        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isDealerChoicePending());
    }

    // ---- after the dealer's choice ----

    @Test
    public void theDealerTakingAnExtraHandHoldingThirtyOneEndsTheDeal() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("AC", "KC", "QC"),
                h("7H", "8D", "9S"), h("AH", "10H", "9D"), h("QS", "JS", "7D"));
        fm.next(state, new ChooseHand(true));
        // the dealer (2) now holds 31 in Clubs; player 1 has 21, player 0 has 9
        assertEndedWith(state, LOSE_GAME, LOSE_GAME, WIN_GAME);
        assertEquals(new HashSet<>(cards("QS", "JS", "7D")), new HashSet<>(state.getTable().getComponents()));
    }

    @Test
    public void theDealerTakingAnExtraHandOfThreeAcesEndsTheDeal() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("AC", "AD", "AS"),
                h("7H", "8D", "9S"), h("AH", "10H", "9D"), h("QS", "JS", "7D"));
        fm.next(state, new ChooseHand(true));
        assertEndedWith(state, LOSE_GAME, LOSE_GAME, WIN_GAME);
    }

    @Test
    public void theDealerKeepingLeavesTheSchnauzOnTheTableAndPlayGoesOn() {
        SchwimmenGameState state = state3();
        arrangeDeal(state, h("AC", "KC", "QC"),
                h("7H", "8D", "9S"), h("AH", "10H", "9D"), h("QS", "JS", "7D"));
        fm.next(state, new ChooseHand(false));
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(new HashSet<>(cards("AC", "KC", "QC")), new HashSet<>(state.getTable().getComponents()));
        // player 0 exchanges the whole hand for the table's 31: the deal ends
        fm.next(state, new ExchangeAll());
        assertEndedWith(state, WIN_GAME, LOSE_GAME, LOSE_GAME);
    }

    // ---- after an exchange ----

    @Test
    public void anExchangeMakingSchnauzEndsTheDealWithNoCloseDecision() {
        SchwimmenGameState state = state3();
        arrangePlay(state, 0, h("JH", "8S", "9C"),
                h("AH", "KH", "7D"), h("7S", "8D", "9H"), h("QC", "10S", "7C"));
        fm.next(state, new ExchangeOne(card("7D"), card("JH")));
        // player 0: 11 + 10 + 10 = 31 in Hearts
        assertEndedWith(state, WIN_GAME, LOSE_GAME, LOSE_GAME);
        assertFalse("no close decision after Schnauz", state.isActionInProgress());
        assertEquals(-1, state.getCloser());
    }

    @Test
    public void exchangingTheWholeHandForThreeAcesEndsTheDeal() {
        SchwimmenGameState state = state3();
        arrangePlay(state, 1, h("AH", "AD", "AC"),
                h("KD", "QS", "8H"), h("7S", "8D", "9H"), h("QC", "10S", "7C"));
        fm.next(state, new ExchangeAll());
        // player 1: Feuer, 32
        assertEndedWith(state, LOSE_GAME, WIN_GAME, LOSE_GAME);
        assertFalse(state.isActionInProgress());
        assertEquals(new HashSet<>(cards("7S", "8D", "9H")), new HashSet<>(state.getTable().getComponents()));
    }

    @Test
    public void theDealerExchangingOneCardForAThirdAceEndsTheDeal() {
        SchwimmenGameState state = state3();
        arrangePlay(state, 2, h("AC", "9D", "7H"),
                h("KD", "QS", "8H"), h("7S", "8D", "9H"), h("AH", "AD", "8S"));
        fm.next(state, new ExchangeOne(card("8S"), card("AC")));
        assertEndedWith(state, LOSE_GAME, LOSE_GAME, WIN_GAME);
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void anExchangeMakingThreeKingsDoesNotEndTheDeal() {
        SchwimmenGameState state = state3();
        arrangePlay(state, 0, h("KS", "8H", "9D"),
                h("KH", "KD", "7C"), h("7S", "8D", "9H"), h("QC", "10S", "7H"));
        fm.next(state, new ExchangeOne(card("7C"), card("KS")));
        assertTrue(state.isNotTerminal());
        assertEquals(30.5, state.getHandValue(0), 1e-9);
        // the turn goes on to the close decision as usual
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(CLOSE_CHOICES, new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void aSchnauzInTheLastRoundAfterACloseEndsTheDealAtOnce() {
        SchwimmenGameState state = state3();
        arrangePlay(state, 0, h("JH", "8S", "9C"),
                h("AH", "KH", "7D"), h("7S", "8D", "9H"), h("QC", "10S", "7C"));
        state.closer = 2;   // the dealer closed: players 0 and 1 have one turn each
        fm.next(state, new ExchangeOne(card("7D"), card("JH")));
        // player 1 does not get their last turn
        assertEndedWith(state, WIN_GAME, LOSE_GAME, LOSE_GAME);
    }

    // ---- integration ----

    @Test
    public void aSchnauzMadeInARealGameEndsItAtOnce() {
        Game game = newGame(4, ordinarySeed(4, 0));
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel gfm = game.getForwardModel();
        gfm.next(state, new ChooseHand(false));
        takeTurn(state, gfm, harmlessExchange(state, gfm));
        takeTurn(state, gfm, new Pass());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());

        // player 2's turn: arrange the hands and table, then player 2 completes 31 in Spades
        arrangePlay(state, 2, h("10S", "8C", "9D"),
                h("KC", "QH", "7D"), h("9H", "8H", "7C"), h("AS", "KS", "7H"), h("JC", "10C", "8D"));
        gfm.next(state, new ExchangeOne(card("7H"), card("10S")));
        // player 2: 11 + 10 + 10 = 31 in Spades; player 3 has 20 in Clubs, player 0 10, player 1 17
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aDealtSchnauzOrFeuerEndsARealGameStraightAfterSetup() {
        // searched, not assumed: the first 8-player seed whose deal gives a player Schnauz or Feuer
        for (long seed = 0; seed < 3000; seed++) {
            Game game = newGame(8, seed);
            SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
            if (!anyPlayerSpecial(state)) continue;
            assertFalse("seed " + seed, state.isNotTerminal());
            assertEquals(GAME_END, state.getGameStatus());
            assertTrue("the dealer never chose", state.isDealerChoicePending());
            for (int p = 0; p < 8; p++)
                assertEquals("player " + p + ", seed " + seed, oracleResult(state, p), state.getPlayerResults()[p]);
            return;
        }
        fail("no 8-player deal with a Schnauz or Feuer in 3000 seeds");
    }

    @Test
    public void aDealtSchnauzInTheExtraHandAloneLetsARealGameStart() {
        // searched, not assumed: the first 8-player seed with Schnauz / Feuer in the extra hand and in no player's
        for (long seed = 0; seed < 5000; seed++) {
            Game game = newGame(8, seed);
            SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
            if (anyPlayerSpecial(state) || !isSpecial(state.getExtraHand().getComponents())) continue;
            assertTrue("seed " + seed, state.isNotTerminal());
            assertEquals(7, state.getCurrentPlayer());
            assertTrue(state.isDealerChoicePending());
            return;
        }
        fail("no 8-player deal with a special extra hand in 5000 seeds");
    }
}
