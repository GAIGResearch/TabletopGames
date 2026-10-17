package games.scopa;

import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.TarotCard.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies, equality and redeterminisation of the state, and value equality of PlayCard.
 */
public class ScopaCopyTest {

    ScopaForwardModel fm;
    ScopaGameState state;

    @Before
    public void setup() {
        fm = new ScopaForwardModel();
        state = newState(31, fm);
        // a position part-way through a deal: cards in both captured piles, a scopa, a last capturer
        arrange(state, 1, of(cup(KING), coin(1)), of(sword(5), sword(6), baton(2)), cup(3), sword(7), baton(4));
        setDeck(state, state.capturedCards.get(0), state.drawDeck, coin(7), cup(7));
        setDeck(state, state.capturedCards.get(1), state.drawDeck, sword(KNAVE), cup(2), coin(KING));
        state.scopas[1] = 1;
        state.lastCapturer = 1;
        assertAllCardsPresent(state);
    }

    @Test
    public void aFaithfulCopyEqualsTheOriginalAndHashesTheSame() {
        ScopaGameState copy = (ScopaGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void equalsAndHashCodeSeeEachCountField() {
        ScopaGameState copy = (ScopaGameState) state.copy();
        copy.lastCapturer = 0;
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());

        copy = (ScopaGameState) state.copy();
        copy.scopas[0]++;
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());

        copy = (ScopaGameState) state.copy();
        copy.bankedScores[1]++;
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void changingACopyLeavesTheOriginalUnchanged() {
        ScopaGameState copy = (ScopaGameState) state.copy();
        copy.table.draw();
        copy.playerHands.get(0).draw();
        copy.capturedCards.get(1).draw();
        copy.drawDeck.draw();
        copy.scopas[1]++;
        copy.lastCapturer = 0;

        assertEquals(setOf(cup(3), sword(7), baton(4)), setOf(state.getTable()));
        assertEquals(setOf(cup(KING), coin(1)), setOf(state.getPlayerHand(0)));
        assertEquals(setOf(sword(KNAVE), cup(2), coin(KING)), setOf(state.getCapturedCards(1)));
        assertEquals(40 - 2 - 3 - 3 - 2 - 3, state.getDrawDeck().getSize());   // 27
        assertEquals(1, state.getScopas(1));
        assertEquals(1, state.getLastCapturer());
        assertNotEquals(state, copy);
    }

    @Test
    public void aPlayersCopyKeepsWhatTheySeeAndReshufflesTheOtherHandWithTheDrawDeck() {
        Set<TarotCard> hidden = new HashSet<>(state.getPlayerHand(1).getComponents());
        hidden.addAll(state.getDrawDeck().getComponents());
        int differs = 0;
        for (int i = 0; i < 10; i++) {
            ScopaGameState copy = (ScopaGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(state.getTable().getComponents(), copy.getTable().getComponents());
            assertEquals(state.getCapturedCards(0).getComponents(), copy.getCapturedCards(0).getComponents());
            assertEquals(state.getCapturedCards(1).getComponents(), copy.getCapturedCards(1).getComponents());
            assertEquals(1, copy.getScopas(1));
            assertEquals(1, copy.getLastCapturer());
            assertEquals(3, copy.getPlayerHand(1).getSize());
            assertEquals(27, copy.getDrawDeck().getSize());
            Set<TarotCard> copyHidden = new HashSet<>(copy.getPlayerHand(1).getComponents());
            copyHidden.addAll(copy.getDrawDeck().getComponents());
            assertEquals(hidden, copyHidden);
            assertAllCardsPresent(copy);
            if (!setOf(copy.getPlayerHand(1)).equals(setOf(state.getPlayerHand(1))))
                differs++;
        }
        assertTrue("player 1's hand was never redeterminised", differs > 0);
    }

    @Test
    public void playCardEqualityDoesNotDependOnTheOrderOfTheCapturedCards() {
        PlayCard a = new PlayCard(cup(KING), List.of(cup(2), cup(3), sword(5)));
        PlayCard b = new PlayCard(cup(KING), List.of(sword(5), cup(2), cup(3)));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new PlayCard(cup(KING), List.of(cup(2), cup(3))));
        assertNotEquals(a, new PlayCard(coin(KING), List.of(cup(2), cup(3), sword(5))));
        assertNotEquals(new PlayCard(cup(KING)), new PlayCard(coin(KING)));
        assertEquals(new PlayCard(cup(KING)), new PlayCard(cup(KING), List.of()));
    }
}
