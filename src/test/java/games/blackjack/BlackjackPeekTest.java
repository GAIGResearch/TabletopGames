package games.blackjack;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.isTenValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The dealer's peek: straight after the deal, under an Ace or ten-value up card, the dealer checks for
 * Blackjack. A dealer Blackjack is turned up and settles the hand before anyone plays (a player natural pushes, every
 * other hand loses); otherwise play goes on and the hole card is known not to make a Blackjack, which
 * redeterminisation must respect. There is no peek under a 2-9 up card.
 */
public class BlackjackPeekTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        newState(3);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    @Test
    public void dealerHasBlackjackWithAnAceAndATenValueCard() {
        setDealer(state, "AS", "KD");
        assertTrue(state.dealerHasBlackjack());
        setDealer(state, "QS", "AD");
        assertTrue(state.dealerHasBlackjack());
        setDealer(state, "10H", "AC");
        assertTrue(state.dealerHasBlackjack());
        setDealer(state, "AS", "9D");      // soft 20
        assertFalse(state.dealerHasBlackjack());
        setDealer(state, "10S", "10D");    // 20
        assertFalse(state.dealerHasBlackjack());
        setDealer(state, "AH", "AD");      // soft 12
        assertFalse(state.dealerHasBlackjack());
    }

    @Test
    public void aDealerBlackjackUnderAnAceEndsTheHandBeforeAnyPlay() {
        // player 0's natural pushes (bet 2 back: 10); players 1 (20) and 2 (14) lose their bets of 4 and 6
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"AH KC", "10S QD", "7C 7D"}, "AS", "KD", "5H");
        declineInsurance(state, fm);     // every player offered insurance declines it

        assertFalse("the hand ends at the peek", state.isNotTerminal());
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(setOf("AS KD"), setOf(state.getDealerHand()));
        assertEquals("the dealer draws nothing", 52 - 8, state.getDrawDeck().getSize());
        assertEquals(card("5H"), state.getDrawDeck().peek());
        assertEquals("nobody played", setOf("AH KC"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("10S QD"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(setOf("7C 7D"), setOf(state.getPlayerHand(2, 0)));
        assertArrayEquals(new int[]{10, 6, 4}, state.chips);
        for (int p = 0; p < 3; p++)
            assertEquals(0, state.getBet(p, 0));
        assertArrayEquals(new Object[]{DRAW_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aDealerBlackjackUnderATenEndsTheHandBeforeAnyPlay() {
        newState(1);
        betAndDeal(state, fm, new int[]{4}, new String[]{"10H 10C"}, "QS", "AD", "5H");
        declineInsurance(state, fm);     // every player offered insurance declines it
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(setOf("QS AD"), setOf(state.getDealerHand()));
        assertEquals(52 - 4, state.getDrawDeck().getSize());
        assertEquals(setOf("10H 10C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(6, state.getChips(0));
        assertEquals(0, state.getBet(0, 0));
        assertArrayEquals(new Object[]{LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void aNaturalPushesAgainstADealerBlackjackInEitherMode() {
        for (boolean naturalOnly : new boolean[]{false, true}) {
            params = new BlackjackParameters();
            params.setRandomSeed(42);
            if (naturalOnly)
                pagatNaturals(params);
            newState(1);
            betAndDeal(state, fm, new int[]{10}, new String[]{"AH JC"}, "10S", "AD", "");
            assertFalse(state.isNotTerminal());
            assertEquals("naturalOnly " + naturalOnly + ": bet returned, not paid", 10, state.getChips(0));
            assertEquals(0, state.getBet(0, 0));
            assertEquals(setOf("10S AD"), setOf(state.getDealerHand()));
            assertArrayEquals(new Object[]{DRAW_GAME}, state.getPlayerResults());
        }
    }

    @Test
    public void withoutADealerBlackjackPlayGoesOnUnderAnAceOrATen() {
        for (String up : new String[]{"AS", "KS"}) {
            newState(3);
            betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, up, "9D", "");
            declineInsurance(state, fm); // every player offered insurance declines it
            assertTrue(up, state.isNotTerminal());
            assertEquals(up, Play, state.getGamePhase());
            assertEquals(up, 0, state.getCurrentPlayer());
            assertEquals(up, setOf("9D"), setOf(state.getHoleCard()));
            assertEquals(up, setOf(up), setOf(state.getDealerHand()));
            assertEquals(up, HIT_OR_STAND, new HashSet<>(fm.computeAvailableActions(state)));
            assertArrayEquals(up, new int[]{8, 6, 4}, state.chips);
        }
    }

    @Test
    public void thereIsNoPeekUnderATwoToNineUpCard() {
        // 9 + A is a soft 20: the hole card stays face down and play starts
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "9H", "AD", "");
        assertTrue(state.isNotTerminal());
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(setOf("AD"), setOf(state.getHoleCard()));
        assertEquals(setOf("9H"), setOf(state.getDealerHand()));
    }

    /**
     * Copies from each player's point of view of a Play-phase position with the hole card face down: every hole card
     * seen, and checks common to all of them (face-up cards kept, the hidden cards are the same set).
     */
    private Set<FrenchCard> holeCardsOverCopies(int nCopies) {
        Set<FrenchCard> hidden = new HashSet<>(state.getDrawDeck().getComponents());
        hidden.addAll(state.getHoleCard().getComponents());
        Set<FrenchCard> seen = new HashSet<>();
        for (int i = 0; i < nCopies; i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy(i % 3);
            assertEquals(state.getDealerHand().getComponents(), copy.getDealerHand().getComponents());
            assertEquals(1, copy.getHoleCard().getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            Set<FrenchCard> copyHidden = new HashSet<>(copy.getDrawDeck().getComponents());
            copyHidden.addAll(copy.getHoleCard().getComponents());
            assertEquals("the draw deck still holds the cards that may not go in the hole", hidden, copyHidden);
            seen.add(copy.getHoleCard().peek());
        }
        return seen;
    }

    @Test
    public void afterAFailedPeekUnderAnAceRedeterminisationNeverPutsATenInTheHole() {
        // arranged as just after the deal and a peek that found no Blackjack (A + 9)
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "AS", "9D", "");
        Set<FrenchCard> seen = holeCardsOverCopies(300);
        for (FrenchCard c : seen)
            assertFalse("ten-value " + c + " in the hole under an Ace", isTenValue(c));
        assertTrue("an Ace may be in the hole under an Ace", seen.stream().anyMatch(c -> c.type == Ace));
        assertTrue("the hole card was hardly redeterminised", seen.size() > 10);
    }

    @Test
    public void afterAFailedPeekUnderATenRedeterminisationNeverPutsAnAceInTheHole() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "KS", "9D", "");
        Set<FrenchCard> seen = holeCardsOverCopies(300);
        for (FrenchCard c : seen)
            assertNotEquals("Ace " + c + " in the hole under a ten", Ace, c.type);
        assertTrue("a ten-value card may be in the hole under a ten", seen.stream().anyMatch(BlackjackGameState::isTenValue));
        assertTrue("the hole card was hardly redeterminised", seen.size() > 10);
    }

    @Test
    public void underATwoToNineUpCardTheHoleCardIsRedeterminisedWithoutConstraint() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "9D", "");
        Set<FrenchCard> seen = holeCardsOverCopies(300);
        assertTrue("an Ace in the hole under a 6", seen.stream().anyMatch(c -> c.type == Ace));
        assertTrue("a ten-value card in the hole under a 6", seen.stream().anyMatch(BlackjackGameState::isTenValue));
    }
}
