package games.blackjack;

import core.actions.AbstractAction;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The doubleDown option. In the Play phase DoubleDown is offered as well as Hit and Stand when the active hand has
 * exactly two cards (any total), is not a natural, and the player has chips of at least
 * the hand's bet. It moves a second bet equal to the first from the chips to the hand, deals exactly one card onto
 * it, and finishes the hand (the turn passes as after a Stand, whatever the total). Settlement is at the doubled
 * stake. Positions are arranged just after the deal, with dealer up cards 2-9 unless a test is about insurance.
 */
public class BlackjackDoubleDownTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());
    static final Set<AbstractAction> HIT_STAND_OR_DOUBLE = Set.of(new Hit(), new Stand(), new DoubleDown());

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        params.setParameterValue("doubleDown", true);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /**
     * Three players: bets 2, 4, 6 (chips left 8, 6, 4) on 10 + 2, 9 + 8 and 7 + 7 against 6 + 10.
     */
    private void threePlayers(String drawDeckTop) {
        newState(3);
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", drawDeckTop);
    }

    // ---------------------------------------------------------------- when it is offered

    @Test
    public void doubleDownIsOfferedOnTwoCardsWithChipsForTheBet() {
        // any two-card total may double: 12 and 17 here. Player 2 bet 6 with 4 left: not offered
        threePlayers("");
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void withTheOptionOffDoubleDownIsNeverOffered() {
        // the pair of the test above
        params.setParameterValue("doubleDown", false);
        threePlayers("");
        assertEquals(HIT_OR_STAND, legalActions());
        fm.next(state, new Stand());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void doubleDownIsNotOfferedOnceTheHandHasThreeCards() {
        // 10 + 2 hits the 5: 17 from three cards
        threePlayers("5H");
        fm.next(state, new Hit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, state.getPlayerHand(0, 0).getSize());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void doubleDownIsNotOfferedOnANatural() {
        // by default a natural still plays, and is offered only Hit or Stand. Player 1 (17) may double
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"AH KC", "9S 8D"}, "6S", "10D", "");
        assertEquals(HIT_OR_STAND, legalActions());
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());
    }

    @Test
    public void doubleDownNeedsChipsOfAtLeastTheBet() {
        // bet 10 of 10: nothing left
        newState(1);
        arrangePlay(state, new int[]{10}, new String[]{"5H 6C"}, "6S", "10D", "");
        assertEquals(HIT_OR_STAND, legalActions());
        // bet 6 of 10: 4 left
        newState(1);
        arrangePlay(state, new int[]{6}, new String[]{"5H 6C"}, "6S", "10D", "");
        assertEquals(HIT_OR_STAND, legalActions());
        // bet 6 of 12: exactly 6 left
        params.setParameterValue("startingChips", 12);
        newState(1);
        arrangePlay(state, new int[]{6}, new String[]{"5H 6C"}, "6S", "10D", "");
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());
    }

    @Test
    public void chipsSpentOnInsuranceCanLeaveTooFewToDouble() {
        // 12 chips, bet 6 on 5 + 6 under the dealer's K + 7 (no Blackjack). Declining insurance leaves 6: may double
        params.setParameterValue("startingChips", 12);
        newState(1);
        betAndDeal(state, fm, new int[]{6}, new String[]{"5H 6C"}, "KS", "7D", "");
        insure(state, fm, false);
        assertEquals(Play, state.getGamePhase());
        assertEquals(6, state.getChips(0));
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());
        // buying it costs 3 and is lost at the peek: 3 left, fewer than the bet
        newState(1);
        betAndDeal(state, fm, new int[]{6}, new String[]{"5H 6C"}, "KS", "7D", "");
        insure(state, fm, true);
        assertEquals(Play, state.getGamePhase());
        assertEquals(3, state.getChips(0));
        assertEquals(HIT_OR_STAND, legalActions());
    }

    // ---------------------------------------------------------------- what it does

    @Test
    public void doublingDoublesTheBetDealsOneCardAndPassesTheTurn() {
        // player 0 (bet 2, 8 left) doubles 10 + 2 and gets the 9; player 1 (bet 4, 6 left) doubles 9 + 8 and gets the 3
        threePlayers("9D 3C 4H");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new DoubleDown());
        assertEquals(4, state.getBet(0, 0));
        assertEquals(6, state.getChips(0));
        assertEquals(setOf("10H 2C 9D"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals(card("3C"), state.getDrawDeck().peek());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions());

        fm.next(state, new DoubleDown());
        assertEquals(8, state.getBet(1, 0));
        assertEquals(2, state.getChips(1));
        assertEquals(setOf("9S 8D 3C"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(card("4H"), state.getDrawDeck().peek());
        assertEquals(2, state.getCurrentPlayer());
        // nothing else changed
        assertEquals(4, state.getBet(0, 0));
        assertEquals(6, state.getChips(0));
        assertEquals(6, state.getBet(2, 0));
        assertEquals(4, state.getChips(2));
        assertEquals(setOf("7C 7D"), setOf(state.getPlayerHand(2, 0)));
        assertEquals(setOf("6S"), setOf(state.getDealerHand()));
        assertEquals(1, state.getHoleCard().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aDoubleOnALowTotalStillFinishesTheHand() {
        // 2 + 3 doubles and gets the 4: 9, and the turn passes anyway
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"2H 3C", "9S 8D"}, "6S", "10D", "4D");
        fm.next(state, new DoubleDown());
        assertEquals(setOf("2H 3C 4D"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
    }

    // ---------------------------------------------------------------- settlement at the doubled stake

    /**
     * One player bets `bet` on `hand` against the dealer's up and hole cards, doubles and gets the first drawDeckTop
     * card; as the only player this ends play, so the dealer plays and the hand is settled. Returns the chips.
     */
    private int onePlayerDoubles(int bet, String hand, String up, String hole, String drawDeckTop) {
        newState(1);
        arrangePlay(state, new int[]{bet}, new String[]{hand}, up, hole, drawDeckTop);
        fm.next(state, new DoubleDown());
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getBet(0, 0));
        assertEquals(0, state.getHoleCard().getSize());
        assertAllCardsPresent(state);
        return state.getChips(0);
    }

    @Test
    public void aWinningDoubleIsPaidEvenMoneyOnTheDoubledBet() {
        // bet 4 doubled to 8 (2 left): 5 + 6 + 9 = 20 beats 10 + 7: 2 + 8 + 8 = 18
        assertEquals(18, onePlayerDoubles(4, "5H 6C", "10S", "7D", "9D 2H"));
        assertEquals(setOf("5H 6C 9D"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("10S 7D"), setOf(state.getDealerHand()));
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void aDoubleThatTiesPushesTheDoubledBet() {
        // bet 2 doubled to 4 (6 left): 20 against 10 + Q: 6 + 4 = 10
        assertEquals(10, onePlayerDoubles(2, "5H 6C", "10S", "QD", "9D 2H"));
        assertArrayEquals(new Object[]{DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void aLosingDoubleLosesTheDoubledBet() {
        // bet 2 doubled to 4: 5 + 4 + 2 = 11 against 17: 6
        assertEquals(6, onePlayerDoubles(2, "5H 4C", "10S", "7D", "2D 9H"));
        assertEquals(setOf("5H 4C 2D"), setOf(state.getPlayerHand(0, 0)));
        assertArrayEquals(new Object[]{LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void aBustOnADoubleLosesTheDoubledBetAndTheDealerDoesNotDraw() {
        // bet 2 doubled to 4: 10 + 6 + K = 26. The dealer's 10 + 5 would otherwise draw the 3
        assertEquals(6, onePlayerDoubles(2, "10H 6C", "10S", "5D", "KD 3H"));
        assertEquals(setOf("10S 5D"), setOf(state.getDealerHand()));
        assertEquals(card("3H"), state.getDrawDeck().peek());
        assertArrayEquals(new Object[]{LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void aDoubleWinsTheDoubledBetWhenTheDealerBusts() {
        // bet 2 doubled to 4: 5 + 4 + 2 = 11; the dealer's 10 + 6 draws the K: 6 + 4 + 4 = 14
        assertEquals(14, onePlayerDoubles(2, "5H 4C", "10S", "6D", "2D KH"));
        assertEquals(setOf("10S 6D KH"), setOf(state.getDealerHand()));
    }

    @Test
    public void byDefaultADoubledTwentyOneIsPaidPayout21OnTheDoubledBet() {
        // bet 2 doubled to 4: 5 + 6 + K = 21 against 17 wins floor(4 x 2.0) = 8: 6 + 4 + 8 = 18
        assertEquals(18, onePlayerDoubles(2, "5H 6C", "10S", "7D", "KD"));
    }

    @Test
    public void withNaturalOnlyADoubledTwentyOneIsPaidEvenMoneyOnTheDoubledBet() {
        // the pair of the test above: a three-card 21 pays 1:1: 6 + 4 + 4 = 14
        pagatNaturals(params);
        assertEquals(14, onePlayerDoubles(2, "5H 6C", "10S", "7D", "KD"));
    }

    // ---------------------------------------------------------------- copies

    @Test
    public void aCopyAfterADoubleIsEqualAndACopyBeforeItIsIndependent() {
        threePlayers("9D 3C");
        BlackjackGameState before = (BlackjackGameState) state.copy();
        int beforeHash = state.hashCode();
        fm.next(state, new DoubleDown());

        BlackjackGameState after = (BlackjackGameState) state.copy();
        assertEquals(state, after);
        assertEquals(state.hashCode(), after.hashCode());
        assertEquals(4, after.getBet(0, 0));
        assertEquals(6, after.getChips(0));
        assertNotEquals(before, state);

        // the copy taken before the double was not changed by it, and doubles on its own
        assertEquals(beforeHash, before.hashCode());
        assertEquals(2, before.getBet(0, 0));
        assertEquals(8, before.getChips(0));
        assertEquals(2, before.getPlayerHand(0, 0).getSize());
        assertEquals(0, before.getCurrentPlayer());
        fm.next(before, new DoubleDown());
        assertEquals(state, before);

        // redeterminised from player 1: the doubled bet, the chips and the face-up cards are kept
        BlackjackGameState view = (BlackjackGameState) state.copy(1);
        assertEquals(state.bets, view.bets);
        assertArrayEquals(state.chips, view.chips);
        for (int p = 0; p < 3; p++)
            assertEquals(state.getPlayerHand(p, 0).getComponents(), view.getPlayerHand(p, 0).getComponents());
        assertEquals(1, view.getCurrentPlayer());
    }
}
