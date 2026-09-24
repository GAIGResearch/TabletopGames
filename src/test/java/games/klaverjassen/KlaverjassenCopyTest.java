package games.klaverjassen;

import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

public class KlaverjassenCopyTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    /**
     * Hearts trumps, one trick played and the second in progress: trick 1 was AS (player 0), 7D (player 1, so known
     * void in spades), 8S, 9S - won by player 0 for AS 11 = 11 card points; player 0 has led KS to trick 2, and player
     * 1 is to play. Player 1 holds only diamonds.
     */
    @Before
    public void setup() {
        state = newState(42);
        fm = new KlaverjassenForwardModel();
        setTrumps(state, Hearts);
        arrangeHands(state,
                cards("7S", "10S", "7H", "AH", "KH", "AC"),
                cards("8D", "9D", "10D", "JD", "QD", "KD", "AD"),
                cards("JS", "8H", "9H", "10H", "7C", "8C", "9C"),
                cards("QS", "JH", "QH", "10C", "JC", "QC", "KC"));
        arrangeTrick(state, 0, "KS");
        state.knownVoids.get(1).add(Spades);
        state.handPoints = new int[]{11, 0};
        state.tricksWon = new int[]{1, 0};
        // arrangement guard
        assertEquals(new HashSet<>(cards("AS", "7D", "8S", "9S")), new HashSet<>(cardsOf(state.getDiscardPile())));
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    private static Set<FrenchCard> otherHands(KlaverjassenGameState s, int observer) {
        Set<FrenchCard> cards = new HashSet<>();
        for (int p = 0; p < 4; p++)
            if (p != observer) cards.addAll(s.getPlayerHand(p).getComponents());
        return cards;
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        KlaverjassenGameState copy = (KlaverjassenGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        int originalHash = state.hashCode();
        fm.next(copy, new PlayCard(card("8D")));    // in the copy player 1, holding no spades, discards a diamond
        copy.handPoints[0] = 99;
        copy.teamScores[1] = 99;
        assertEquals(originalHash, state.hashCode());
        assertEquals(cards("KS"), cardsOf(state.getCurrentTrick()));
        assertEquals(7, state.getPlayerHand(1).getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertArrayEquals(new int[]{11, 0}, state.handPoints);
        assertArrayEquals(new int[]{0, 0}, state.teamScores);
        assertNotEquals(state, copy);
    }

    @Test
    public void equalityCoversEveryPartOfTheHand() {
        KlaverjassenGameState copy;

        copy = (KlaverjassenGameState) state.copy();
        copy.trumpSuit = Clubs;
        assertNotEquals("trump suit", state, copy);

        copy = (KlaverjassenGameState) state.copy();
        copy.handPoints[1] = 5;
        assertNotEquals("hand points", state, copy);

        copy = (KlaverjassenGameState) state.copy();
        copy.handRoem[0] = 20;
        assertNotEquals("roem", state, copy);

        copy = (KlaverjassenGameState) state.copy();
        copy.tricksWon[1] = 1;
        assertNotEquals("tricks won", state, copy);

        copy = (KlaverjassenGameState) state.copy();
        copy.teamScores[0] = 1;
        assertNotEquals("team scores", state, copy);

        copy = (KlaverjassenGameState) state.copy();
        copy.knownVoids.get(2).add(Diamonds);
        assertNotEquals("known voids", state, copy);
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerSeesAndNeverBreaksAKnownVoid() {
        int p1Changed = 0;
        for (int i = 0; i < 50; i++) {
            KlaverjassenGameState copy = (KlaverjassenGameState) state.copy(0);
            assertEquals(cardsOf(state.getPlayerHand(0)), cardsOf(copy.getPlayerHand(0)));
            assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(copy.getCurrentTrick()));
            assertEquals(cardsOf(state.getDiscardPile()), cardsOf(copy.getDiscardPile()));
            assertEquals(6, copy.getPlayerHand(0).getSize());
            for (int p = 1; p < 4; p++)
                assertEquals(7, copy.getPlayerHand(p).getSize());
            assertEquals(otherHands(state, 0), otherHands(copy, 0));
            assertAllCardsPresent(copy);
            assertEquals(Hearts, copy.getTrumpSuit());
            assertArrayEquals(new int[]{11, 0}, copy.handPoints);
            assertArrayEquals(new int[]{1, 0}, copy.tricksWon);

            // player 1 is known void in spades: never dealt one
            for (FrenchCard c : copy.getPlayerHand(1).getComponents())
                assertNotEquals("player 1 given " + c, Spades, c.suite);

            if (!new HashSet<>(cardsOf(copy.getPlayerHand(1))).equals(new HashSet<>(cardsOf(state.getPlayerHand(1)))))
                p1Changed++;
        }
        // the allowed (non-spade) cards still reach player 1
        assertTrue("player 1's hand was never redeterminised", p1Changed > 0);
    }

    @Test
    public void withNoVoidKnownAHiddenHandCanReceiveAnySuit() {
        state.knownVoids.clear();
        // player 1 holds no spades, but nothing is known: the hidden JS and QS sometimes reach player 1
        int spadesToPlayer1 = 0;
        for (int i = 0; i < 50; i++) {
            KlaverjassenGameState copy = (KlaverjassenGameState) state.copy(0);
            if (copy.getPlayerHand(1).getComponents().stream().anyMatch(c -> c.suite == Spades))
                spadesToPlayer1++;
        }
        assertTrue("player 1 never given a spade", spadesToPlayer1 > 0);
    }
}
