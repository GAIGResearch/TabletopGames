package games.gofish;

import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * Which cards in the hands are visible to whom after an ask. Three players; P0 to play.
 * Every ask shows one card of the rank the asker holds (unless one already is shown); cards handed over are shown;
 * a card drawn after "Go fish" is private unless it is the rank asked for and gives another turn; a card's visibility
 * travels with it.
 */
public class GoFishVisibilityTest {

    GoFishParameters params;
    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        params = new GoFishParameters();
        state = newState(params, 3, 17);
        fm = new GoFishForwardModel();
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("5C"), card("9D"));
        giveHand(state, 2, card("7H"), card("2C"));
        stackDrawDeck(state, card("8D"));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(52 - 2 - 3 - 2, state.drawDeck.getSize());   // 45
        // the deal shows nothing: every card is visible to its owner only
        for (int p = 0; p < 3; p++)
            for (var c : state.playerHands.get(p).getComponents())
                assertVisibleOnlyToOwner(state, p, c);
    }

    // ---- Rule 1: every ask shows one card of the rank held by the asker ----

    @Test
    public void aFailedAskShowsTheAskersCardOfTheRank() {
        fm.next(state, new GoFishAsk(2, 5));     // P2 holds no 5: Go fish, P0 draws the 8D
        assertVisibleToAll(state, 0, card("5H"));
        assertVisibleOnlyToOwner(state, 0, card("KD"));
    }

    @Test
    public void anAskShowsExactlyOneOfTheAskersCardsOfTheRank() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        fm.next(state, new GoFishAsk(2, 5));     // Go fish
        assertEquals(cards("5H", "5D", "KD", "8D"), cardSet(state.playerHands.get(0)));
        assertEquals("one of the 5H / 5D is shown, not both", 1, visibleToAllOfRank(state, 0, 5));
        // and the other stays with its owner
        boolean hShown = isVisibleToAll(state, 0, card("5H"));
        assertVisibleOnlyToOwner(state, 0, card(hShown ? "5D" : "5H"));
    }

    @Test
    public void anAskShowsNothingMoreWhenACardOfTheRankIsAlreadyShown() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        showToAll(state, 0, card("5H"));
        fm.next(state, new GoFishAsk(2, 5));     // Go fish
        assertVisibleToAll(state, 0, card("5H"));
        assertVisibleOnlyToOwner(state, 0, card("5D"));
        assertEquals(1, visibleToAllOfRank(state, 0, 5));
    }

    // ---- Rules 1 and 2: a successful ask ----

    @Test
    public void aSuccessfulAskShowsTheAskersOneCardAndTheCardsHandedOver() {
        fm.next(state, new GoFishAsk(1, 5));     // P1 hands over 5S 5C
        assertEquals(cards("5H", "KD", "5S", "5C"), cardSet(state.playerHands.get(0)));
        // the 5H held before (rule 1) + the 5S and 5C handed over (rule 2) = 3
        assertVisibleToAll(state, 0, card("5H"));
        assertVisibleToAll(state, 0, card("5S"));
        assertVisibleToAll(state, 0, card("5C"));
        assertVisibleOnlyToOwner(state, 0, card("KD"));
    }

    @Test
    public void aSuccessfulAskWhenTheAskerHadTwoShowsOneOfThemAndTheCardsHandedOver() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        giveHand(state, 1, card("5S"), card("9D"));
        fm.next(state, new GoFishAsk(1, 5));     // P1 hands over the 5S; 3 fives, no book
        assertEquals(cards("5H", "5D", "KD", "5S"), cardSet(state.playerHands.get(0)));
        assertVisibleToAll(state, 0, card("5S"));
        // one of the two held before (rule 1) + the 5S handed over (rule 2) = 2 of the 3 fives
        assertEquals(1 + 1, visibleToAllOfRank(state, 0, 5));
        assertVisibleOnlyToOwner(state, 0, card("KD"));
    }

    @Test
    public void aSuccessfulAskKeepsAShownCardShownAndShowsNoOtherOfTheAskersOwn() {
        giveHand(state, 0, card("5H"), card("5D"), card("KD"));
        giveHand(state, 1, card("5S"), card("9D"));
        showToAll(state, 0, card("5H"));
        fm.next(state, new GoFishAsk(1, 5));
        assertVisibleToAll(state, 0, card("5H"));
        assertVisibleOnlyToOwner(state, 0, card("5D"));
        assertVisibleToAll(state, 0, card("5S"));
        assertEquals("the 5H shown before + the 5S handed over", 2, visibleToAllOfRank(state, 0, 5));
    }

    // ---- Rule 3: the card drawn after "Go fish" ----

    @Test
    public void aDrawnCardOfAnotherRankIsVisibleOnlyToTheDrawer() {
        fm.next(state, new GoFishAsk(2, 5));     // draws the 8D
        assertEquals(cards("5H", "KD", "8D"), cardSet(state.playerHands.get(0)));
        assertVisibleOnlyToOwner(state, 0, card("8D"));
    }

    @Test
    public void aDrawnCardOfTheRankAskedForIsShownWhenItGivesAnotherTurn() {
        stackDrawDeck(state, card("5D"));
        fm.next(state, new GoFishAsk(2, 5));     // draws the 5D: the rank asked for, continueOnDrawingSameRank true
        assertEquals(0, state.getCurrentPlayer());
        assertVisibleToAll(state, 0, card("5D"));
        assertVisibleToAll(state, 0, card("5H"));   // rule 1 still applies
    }

    @Test
    public void aDrawnCardOfTheRankAskedForStaysPrivateWhenItGivesNoOtherTurn() {
        params.setParameterValue("continueOnDrawingSameRank", false);
        stackDrawDeck(state, card("5D"));
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(1, state.getCurrentPlayer());
        assertVisibleOnlyToOwner(state, 0, card("5D"));
        assertVisibleToAll(state, 0, card("5H"));   // rule 1 still applies
    }

    // ---- Rule 4: visibility travels with the card ----

    @Test
    public void aShownCardStaysShownAfterADraw() {
        showToAll(state, 0, card("KD"));
        fm.next(state, new GoFishAsk(2, 5));     // the 8D drawn goes into the hand, shifting positions
        assertVisibleToAll(state, 0, card("KD"));
        assertVisibleToAll(state, 0, card("5H"));
        assertVisibleOnlyToOwner(state, 0, card("8D"));
    }

    @Test
    public void theTargetsRemainingCardsKeepTheirVisibility() {
        giveHand(state, 1, card("5S"), card("9D"), card("KC"), card("5C"));
        showToAll(state, 1, card("KC"));
        fm.next(state, new GoFishAsk(1, 5));     // 5S and 5C leave P1's hand from either side of the others
        assertEquals(cards("9D", "KC"), cardSet(state.playerHands.get(1)));
        assertVisibleToAll(state, 1, card("KC"));
        assertVisibleOnlyToOwner(state, 1, card("9D"));
    }

    @Test
    public void layingDownABookLeavesTheVisibilityOfTheOtherCardsInHand() {
        giveHand(state, 0, card("7C"), card("5H"), card("KD"), card("5D"));
        showToAll(state, 0, card("KD"));
        fm.next(state, new GoFishAsk(1, 5));     // 5H 5D + 5S 5C = a book of 5s, laid down
        assertEquals(cards("5H", "5D", "5S", "5C"), cardSet(state.playerBooks.get(0)));
        assertEquals(cards("7C", "KD"), cardSet(state.playerHands.get(0)));
        assertVisibleToAll(state, 0, card("KD"));
        assertVisibleOnlyToOwner(state, 0, card("7C"));
    }
}
