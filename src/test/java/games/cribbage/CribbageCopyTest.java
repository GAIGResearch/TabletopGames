package games.cribbage;

import core.components.FrenchCard;
import games.cribbage.actions.DiscardToCrib;
import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies, equality and redeterminisation.
 */
public class CribbageCopyTest {

    CribbageParameters params;
    CribbageGameState state;
    CribbageForwardModel fm;

    @Before
    public void setup() {
        params = new CribbageParameters();
        params.setRandomSeed(42);
        state = new CribbageGameState(params, 2);
        fm = new CribbageForwardModel();
        fm.setup(state);
        giveHand(state, 1, cards("10H", "4C", "8S", "7D", "QC", "AH"));
        giveHand(state, 0, cards("9H", "5C", "3S", "KD", "6D", "2C"));
        putOnTopOfDrawDeck(state, card("9D"));
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        CribbageGameState copy = (CribbageGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        int originalHash = state.hashCode();
        fm.next(copy, new DiscardToCrib(card("QC"), card("AH")));
        assertEquals(originalHash, state.hashCode());
        assertEquals(6, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getCrib().getSize());
        assertNotEquals(state, copy);
    }

    @Test
    public void fullCopyDuringThePlayIsEqual() {
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));
        fm.next(state, new PlayCard(card("4C")));
        CribbageGameState copy = (CribbageGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(List.of(card("4C")), copy.getPlaySequence());
        assertEquals(card("9D"), copy.getStarter());
    }

    private static Set<FrenchCard> unseenBy1(CribbageGameState s) {
        // player 0's hand, player 0's crib cards and the draw deck
        Set<FrenchCard> cards = new HashSet<>(s.getPlayerHand(0).getComponents());
        cards.addAll(s.getDrawDeck().getComponents());
        for (int i = 0; i < s.getCrib().getSize(); i++)
            if (!s.getCrib().isComponentVisible(i, 1))
                cards.add(s.getCrib().get(i));
        return cards;
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerCanSeeAndShufflesTheRest() {
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));
        fm.next(state, new PlayCard(card("4C")));
        fm.next(state, new PlayCard(card("KD")));
        assertEquals(Set.copyOf(cards("9H", "5C", "3S")), new HashSet<>(state.getPlayerHand(0).getComponents()));

        int opponentHandChanged = 0, opponentCribChanged = 0;
        for (int i = 0; i < 50; i++) {
            CribbageGameState copy = (CribbageGameState) state.copy(1);
            // what player 1 can see is kept
            assertEquals(state.getPlayerHand(1).getComponents(), copy.getPlayerHand(1).getComponents());
            assertTrue(copy.getCrib().getComponents().containsAll(cards("QC", "AH")));
            assertEquals(state.getPlayedCards(0).getComponents(), copy.getPlayedCards(0).getComponents());
            assertEquals(state.getPlayedCards(1).getComponents(), copy.getPlayedCards(1).getComponents());
            assertEquals(state.getPlaySequence(), copy.getPlaySequence());
            assertEquals(card("9D"), copy.getStarter());
            assertEquals(state.getScore(0), copy.getScore(0));
            assertEquals(state.getScore(1), copy.getScore(1));
            // what player 1 cannot see is shuffled among the same places, keeping sizes
            assertEquals(3, copy.getPlayerHand(0).getSize());
            assertEquals(4, copy.getCrib().getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            assertEquals(unseenBy1(state), unseenBy1(copy));
            assertAllCardsPresent(copy);

            if (!new HashSet<>(copy.getPlayerHand(0).getComponents()).equals(new HashSet<>(state.getPlayerHand(0).getComponents())))
                opponentHandChanged++;
            if (!copy.getCrib().getComponents().containsAll(cards("6D", "2C")))
                opponentCribChanged++;
        }
        assertTrue("player 0's hand was never redeterminised", opponentHandChanged > 0);
        assertTrue("player 0's crib cards were never redeterminised", opponentCribChanged > 0);
    }
}
