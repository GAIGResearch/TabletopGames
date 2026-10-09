package games.sueca;

import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies of the state. The position: DEAL (Hearts trumps, trump card 2H in player 3's hand) after the first trick
 * AS 3S KS 5S (won by player 0's Ace, on team 0's pile), with player 0 having led KD to the second trick; player 1
 * is to play.
 */
public class SuecaCopyTest {

    SuecaGameState state;
    SuecaForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new SuecaForwardModel();
        arrangeDeal(state);
        arrangeTrick(state, 0, "AS", "3S", "KS", "5S");   // the first trick's cards...
        arrangeTrick(state, 0, "KD");                      // ...go to team 0's pile when the second is arranged
        // arrangement guard
        assertEquals(new HashSet<>(cards("AS", "3S", "KS", "5S")), new HashSet<>(cardsOf(state.getTeamPile(0))));
        assertEquals(0, state.getTeamPile(1).getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(8, state.getPlayerHand(0).getSize());
        for (int p = 1; p < 4; p++)
            assertEquals(9, state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);
    }

    private static Set<FrenchCard> otherHands(SuecaGameState s, int observer) {
        Set<FrenchCard> result = new HashSet<>();
        for (int p = 0; p < 4; p++)
            if (p != observer) result.addAll(s.getPlayerHand(p).getComponents());
        return result;
    }

    @Test
    public void fullCopyIsEqualAndHashesTheSame() {
        SuecaGameState copy = (SuecaGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void changesToACopyDoNotAffectTheOriginal() {
        SuecaGameState copy = (SuecaGameState) state.copy();
        int originalHash = state.hashCode();
        List<FrenchCard> hand1 = cardsOf(state.getPlayerHand(1));
        new PlayCard<>(card("QD")).execute(copy);        // player 1 follows diamonds in the copy only
        copy.getTeamPile(1).add(card("2C"));
        copy.trumpCard = card("AC");

        assertEquals(originalHash, state.hashCode());
        assertEquals(cards("KD"), cardsOf(state.getCurrentTrick()));
        assertEquals(hand1, cardsOf(state.getPlayerHand(1)));
        assertEquals(0, state.getTeamPile(1).getSize());
        assertEquals(card("2H"), state.getTrumpCard());
        assertNotEquals(state, copy);
    }

    @Test
    public void equalityCoversTheTrumpCardAndTheTeamPiles() {
        SuecaGameState copy = (SuecaGameState) state.copy();
        copy.trumpCard = card("7H");                      // same suit, different card
        assertNotEquals("trump card", state, copy);

        copy = (SuecaGameState) state.copy();
        FrenchCard moved = copy.getTeamPile(0).draw();
        copy.getTeamPile(1).add(moved);
        assertNotEquals("team piles", state, copy);
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerSeesAndShufflesTheOtherHands() {
        for (int observer = 0; observer < 4; observer++) {
            int changed = 0;
            for (int i = 0; i < 20; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertEquals(cardsOf(state.getPlayerHand(observer)), cardsOf(copy.getPlayerHand(observer)));
                assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(copy.getCurrentTrick()));
                assertEquals(0, copy.getCurrentTrick().getLeader());
                assertEquals(cardsOf(state.getTeamPile(0)), cardsOf(copy.getTeamPile(0)));
                assertEquals(cardsOf(state.getTeamPile(1)), cardsOf(copy.getTeamPile(1)));
                for (int p = 0; p < 4; p++)
                    assertEquals(state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
                assertEquals(otherHands(state, observer), otherHands(copy, observer));
                assertEquals(card("2H"), copy.getTrumpCard());
                assertEquals(1, copy.getCurrentPlayer());
                assertAllCardsPresent(copy);
                int other = (observer + 1) % 4;
                if (!new HashSet<>(cardsOf(copy.getPlayerHand(other))).equals(new HashSet<>(cardsOf(state.getPlayerHand(other)))))
                    changed++;
            }
            assertTrue("observer " + observer + ": the other hands were never redeterminised", changed > 0);
        }
    }
}
