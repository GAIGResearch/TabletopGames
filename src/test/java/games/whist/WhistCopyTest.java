package games.whist;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.Hearts;
import static games.tricktaking.TrickTakingTestUtils.*;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

public class WhistCopyTest {

    WhistGameState state;
    WhistForwardModel fm;

    /**
     * A full deal with player 1 holding no hearts (all 13 diamonds), spades trumps and the Ace of Spades turned up in
     * dealer 3's hand.
     */
    @Before
    public void setup() {
        state = newState(42);
        fm = new WhistForwardModel();
        arrangeHands(state,
                cards("AH", "KH", "QH", "JH", "10H", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C"),
                cards("AD", "KD", "QD", "JD", "10D", "9D", "8D", "7D", "6D", "5D", "4D", "3D", "2D"),
                cards("9H", "8H", "7H", "6H", "10C", "JC", "QC", "KC", "AC", "2S", "3S", "4S", "5S"),
                cards("5H", "4H", "3H", "2H", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS"));
        setTrumps(state, "AS");
        assertAllCardsPresent(state);
    }

    /**
     * Trick 1: hearts led, player 1 discards a diamond (so is known void in hearts), player 0's Ace wins; player 0
     * then leads the 2 of Clubs, so the trick in progress and the discard pile are both non-empty.
     */
    private void playATrickAndALead() {
        playCards(state, fm, "AH", "2D", "9H", "5H", "2C");
        // arrangement guard
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
        assertEquals(cards("2C"), cardsOf(state.getCurrentTrick()));
        assertEquals(4, state.getDiscardPile().getSize());
        assertEquals(1, state.getCurrentPlayer());
    }

    private static Set<FrenchCard> otherHands(WhistGameState s, int observer) {
        Set<FrenchCard> cards = new HashSet<>();
        for (int p = 0; p < 4; p++)
            if (p != observer) cards.addAll(s.getPlayerHand(p).getComponents());
        return cards;
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        playATrickAndALead();
        WhistGameState copy = (WhistGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        int originalHash = state.hashCode();
        fm.next(copy, new PlayCard(card("3D")));    // in the copy player 1, holding no clubs, discards a diamond
        assertEquals(originalHash, state.hashCode());
        assertEquals(cards("2C"), cardsOf(state.getCurrentTrick()));
        assertEquals(12, state.getPlayerHand(1).getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));   // the copy's new club void is not shared
        assertNotEquals(state, copy);
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerSeesAndNeverBreaksAKnownVoid() {
        playATrickAndALead();
        int p1Changed = 0;
        for (int i = 0; i < 50; i++) {
            WhistGameState copy = (WhistGameState) state.copy(0);
            assertEquals(cardsOf(state.getPlayerHand(0)), cardsOf(copy.getPlayerHand(0)));
            assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(copy.getCurrentTrick()));
            assertEquals(cardsOf(state.getDiscardPile()), cardsOf(copy.getDiscardPile()));
            for (int p = 1; p < 4; p++)
                assertEquals(12, copy.getPlayerHand(p).getSize());
            assertEquals(otherHands(state, 0), otherHands(copy, 0));
            assertAllCardsPresent(copy);

            // player 1 is known void in hearts: never dealt one
            for (FrenchCard c : copy.getPlayerHand(1).getComponents())
                assertNotEquals("player 1 given " + c, Hearts, c.suite);
            // dealer 3 still holds the turned-up Ace of Spades
            assertTrue(copy.getPlayerHand(3).contains(card("AS")));

            if (!new HashSet<>(cardsOf(copy.getPlayerHand(1))).equals(new HashSet<>(cardsOf(state.getPlayerHand(1)))))
                p1Changed++;
        }
        // the allowed (non-heart) cards still reach player 1
        assertTrue("player 1's hand was never redeterminised", p1Changed > 0);
    }

    @Test
    public void withNoVoidKnownAHiddenHandCanReceiveAnySuit() {
        // at the start of the deal nothing is known: player 1's hand (no hearts) is sometimes given a heart
        int heartsToPlayer1 = 0;
        for (int i = 0; i < 50; i++) {
            WhistGameState copy = (WhistGameState) state.copy(0);
            if (copy.getPlayerHand(1).getComponents().stream().anyMatch(c -> c.suite == Hearts))
                heartsToPlayer1++;
        }
        assertTrue("player 1 never given a heart", heartsToPlayer1 > 0);
    }

    @Test
    public void theTurnedUpCardStaysWithTheDealerOnlyUntilPlayed() {
        // hearts trumps instead, the 5 of Hearts turned up in dealer 3's hand
        setTrumps(state, "5H");
        List<FrenchCard> dealerOthersSeen = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            WhistGameState copy = (WhistGameState) state.copy(1);   // seen by a non-dealer
            assertTrue(copy.getPlayerHand(3).contains(card("5H")));
            dealerOthersSeen.addAll(copy.getPlayerHand(3).getComponents());
        }
        // the rest of the dealer's hand is redeterminised
        assertTrue(new HashSet<>(dealerOthersSeen).size() > 13);

        playATrickAndALead();                        // the dealer plays the 5 of Hearts to trick 1
        for (int i = 0; i < 20; i++) {
            WhistGameState copy = (WhistGameState) state.copy(1);
            for (int p = 0; p < 4; p++)
                assertFalse("player " + p + " holds the played trump card", copy.getPlayerHand(p).contains(card("5H")));
            assertEquals(12, copy.getPlayerHand(3).getSize());
            assertTrue(copy.getDiscardPile().contains(card("5H")));
        }
    }
}
