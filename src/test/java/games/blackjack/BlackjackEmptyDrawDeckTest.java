package games.blackjack;

import core.components.FrenchCard;
import games.blackjack.actions.Bet;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Split;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The empty draw deck: whenever a card must be dealt (the deal, Hit, DoubleDown, Split, the dealer's draws)
 * and the draw deck is empty, a fresh 52-card pack shuffled with the game's random generator is first added to the draw deck.
 * The cards in play are then 52 x the packs used, every card the same number of times; at the next hand all of them
 * are gathered into the draw deck.
 * <p>
 * A nearly empty draw deck is arranged by moving the rest of the draw deck into the hand of a player who has finished (a
 * bust hand of 40-odd cards, which just loses) or who sits out, so that the 52 cards stay in play.
 */
public class BlackjackEmptyDrawDeckTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        params.setParameterValue("splitting", true);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    /**
     * Two players, bets 2. Player 0 (10 + 7) has finished; player 1 has p1Hand and is to play. The dealer has 6 + 10.
     * Only drawDeckTop is left in the draw deck: every other card is moved into player 0's hand.
     */
    private void playerOneToPlayWithOnly(String p1Hand, String drawDeckTop) {
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"10H 7C", p1Hand}, "6S", "10D", drawDeckTop);
        leaveInDrawDeck(state, drawDeckTop.isBlank() ? 0 : cards(drawDeckTop).size(), state.getPlayerHand(0, 0));
        state.setTurnOwner(1);
        assertAllCardsPresent(state);
    }

    private static List<FrenchCard> drawnThenDrawDeck(FrenchCard drawn, BlackjackGameState s) {
        List<FrenchCard> order = new ArrayList<>();
        order.add(drawn);
        order.addAll(s.getDrawDeck().getComponents());
        return order;
    }

    // ---------------------------------------------------------------- drawCard

    @Test
    public void drawCardTakesTheTopCardWhileThereIsOne() {
        newState(1);
        stackDrawDeck(state, "5C 9D");
        leaveInDrawDeck(state, 2, state.getPlayerHand(0, 0));
        assertEquals(card("5C"), state.drawCard());
        assertEquals(1, state.getDrawDeck().getSize());
        assertEquals(card("9D"), state.drawCard());
        assertEquals(0, state.getDrawDeck().getSize());
    }

    @Test
    public void drawCardOnAnEmptyDrawDeckAddsAFreshShuffledPack() {
        newState(1);
        leaveInDrawDeck(state, 0, state.getPlayerHand(0, 0));
        FrenchCard drawn = state.drawCard();
        assertNotNull(drawn);
        // the drawn card and the 51 left are a whole pack, face down, shuffled (not in generateDeck order)
        assertEquals(51, state.getDrawDeck().getSize());
        assertEquals(HIDDEN_TO_ALL, state.getDrawDeck().getVisibilityMode());
        List<FrenchCard> pack = drawnThenDrawDeck(drawn, state);
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(pack));
        assertFalse("the new pack is in generateDeck order", keepsRelativeOrder(FULL_DECK, pack));
        state.getDealerHand().add(drawn);
        assertEquals(2, assertCardsConserved(state));

        // shuffled with the game's random generator: the same seed and arrangement give the same pack order
        newState(1);
        leaveInDrawDeck(state, 0, state.getPlayerHand(0, 0));
        FrenchCard drawnAgain = state.drawCard();
        assertEquals(pack, drawnThenDrawDeck(drawnAgain, state));
    }

    // ---------------------------------------------------------------- every way a card is dealt

    @Test
    public void aHitOnAnEmptyDrawDeckTakesTheTopCardOfAFreshPack() {
        // player 1 has 2 + 3, so any card leaves them playing
        playerOneToPlayWithOnly("2S 3C", "");
        fm.next(state, new Hit());
        assertEquals(3, state.getPlayerHand(1, 0).getSize());
        assertTrue(setOf(state.getPlayerHand(1, 0)).containsAll(setOf("2S 3C")));
        assertEquals(51, state.getDrawDeck().getSize());
        assertEquals(2, assertCardsConserved(state));
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void aSplitRunsThroughTheLastCardIntoAFreshPack() {
        // one card left: the first hand gets the 5C, the second hand the top card of the new pack
        playerOneToPlayWithOnly("8H 8D", "5C");
        fm.next(state, new Split());
        assertEquals(2, state.getPlayerHands(1).size());
        assertEquals(setOf("8H 5C"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(2, state.getPlayerHand(1, 1).getSize());
        assertTrue(state.getPlayerHand(1, 1).getComponents().contains(card("8D")));
        assertEquals(List.of(2, 2), state.bets.get(1));
        assertEquals(51, state.getDrawDeck().getSize());
        assertEquals(2, assertCardsConserved(state));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
    }

    @Test
    public void aDoubleDownOnAnEmptyDrawDeckTakesACardFromAFreshPack() {
        // player 1 is the last to play, so the dealer plays and the hand is settled
        params.setParameterValue("doubleDown", true);
        playerOneToPlayWithOnly("5H 6C", "");
        fm.next(state, new DoubleDown());
        assertEquals(3, state.getPlayerHand(1, 0).getSize());
        assertFalse(state.isNotTerminal());
        assertTrue(state.getDrawDeck().getSize() <= 51);
        assertEquals(2, assertCardsConserved(state));
    }

    @Test
    public void theDealerDrawsFromAFreshPackWhenTheDrawDeckIsEmpty() {
        // player 1 stands on 18; the dealer's 16 must draw
        playerOneToPlayWithOnly("10S 8C", "");
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertTrue(state.getDealerHand().getSize() >= 3);
        // one new pack, from which the dealer's draws came
        assertEquals(52 - (state.getDealerHand().getSize() - 2), state.getDrawDeck().getSize());
        assertEquals(2, assertCardsConserved(state));
    }

    @Test
    public void theDealRunsThroughTheLastCardIntoAFreshPack() {
        // player 1 has no chips and sits out; only 3 cards are left for player 0's two and the up card, so the hole
        // card comes from a new pack
        newState(2);
        state.chips[1] = 0;
        stackDrawDeck(state, "9H 6S 7C");
        leaveInDrawDeck(state, 3, state.getPlayerHand(1, 0));
        fm.next(state, new Bet(2));
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(setOf("9H 7C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("6S"), setOf(state.getDealerHand()));
        assertEquals(1, state.getHoleCard().getSize());
        assertEquals(51, state.getDrawDeck().getSize());
        assertEquals(2, assertCardsConserved(state));
    }

    // ---------------------------------------------------------------- afterwards

    @Test
    public void theNextHandGathersEveryPackIntoTheDrawDeck() {
        // nHands 2: player 1 hits through the empty draw deck and stands; the hand is settled and the next one starts
        // with all 104 cards in the draw deck. Player 0's bust hand has lost: 8 chips, first to bet
        params.setParameterValue("nHands", 2);
        playerOneToPlayWithOnly("2S 3C", "");
        fm.next(state, new Hit());
        fm.next(state, new Stand());
        assertNewHandStarted(state, 1, 0, 2);
        assertEquals(8, state.getChips(0));
    }

    @Test
    public void copiesKeepTheExtraPack() {
        playerOneToPlayWithOnly("2S 3C", "");
        fm.next(state, new Hit());
        BlackjackGameState copy = (BlackjackGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(2, assertCardsConserved(copy));
        // redeterminised from player 0: still two packs, the same draw deck size and face-up cards
        BlackjackGameState view = (BlackjackGameState) state.copy(0);
        assertEquals(2, assertCardsConserved(view));
        assertEquals(51, view.getDrawDeck().getSize());
        assertEquals(state.getPlayerHand(1, 0).getComponents(), view.getPlayerHand(1, 0).getComponents());
    }
}
