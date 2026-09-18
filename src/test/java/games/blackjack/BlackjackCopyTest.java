package games.blackjack;

import core.components.FrenchCard;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

public class BlackjackCopyTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 3);
        fm = new BlackjackForwardModel();
        fm.setup(state);
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", "");
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        BlackjackGameState copy = (BlackjackGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(state.getHoleCard().getComponents(), copy.getHoleCard().getComponents());
        assertEquals(state.getDrawDeck().getComponents(), copy.getDrawDeck().getComponents());

        int originalHash = state.hashCode();
        copy.chips[1] = 99;
        copy.bets.get(2).set(0, 8);
        copy.activeHand = 1;
        copy.insurance[0] = 1;
        copy.getPlayerHand(0, 0).add(copy.getDrawDeck().draw());
        assertEquals(originalHash, state.hashCode());
        assertEquals(0, state.getInsurance(0));
        assertEquals(6, state.getChips(1));
        assertEquals(6, state.getBet(2, 0));
        assertEquals(0, state.getActiveHand());
        assertEquals(2, state.getPlayerHand(0, 0).getSize());
        assertNotEquals(state, copy);
    }

    @Test
    public void everyFieldIsPartOfEqualsAndHashCode() {
        List<Consumer<BlackjackGameState>> changes = List.of(
                s -> s.chips[1] = 99,
                s -> s.bets.get(2).set(0, 8),
                s -> s.activeHand = 1,
                s -> s.insurance[1] = 2,
                s -> s.getPlayerHand(0, 0).add(s.getDrawDeck().draw()),
                s -> s.getDealerHand().add(s.getDrawDeck().draw()),
                s -> s.getHoleCard().add(s.getDrawDeck().draw()));
        for (int i = 0; i < changes.size(); i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy();
            changes.get(i).accept(copy);
            assertNotEquals("change " + i, state, copy);
            assertNotEquals("change " + i, state.hashCode(), copy.hashCode());
        }
    }

    @Test
    public void aCopyPlaysOnWithoutChangingTheOriginal() {
        BlackjackGameState copy = (BlackjackGameState) state.copy();
        int originalHash = state.hashCode();
        fm.next(copy, new Stand());
        assertEquals(1, copy.getCurrentPlayer());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(originalHash, state.hashCode());
        assertNotEquals(state, copy);
    }

    private static Set<FrenchCard> hidden(BlackjackGameState s) {
        Set<FrenchCard> cards = new HashSet<>(s.getDrawDeck().getComponents());
        cards.addAll(s.getHoleCard().getComponents());
        return cards;
    }

    @Test
    public void redeterminisationShufflesTheHoleCardWithTheDrawDeckAndKeepsFaceUpCards() {
        int holeChanged = 0, drawDeckChanged = 0;
        for (int i = 0; i < 20; i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy(1);
            for (int p = 0; p < 3; p++)
                assertEquals(state.getPlayerHand(p, 0).getComponents(), copy.getPlayerHand(p, 0).getComponents());
            assertEquals(state.getDealerHand().getComponents(), copy.getDealerHand().getComponents());
            assertArrayEquals(state.chips, copy.chips);
            assertEquals(state.bets, copy.bets);
            assertEquals(state.getGamePhase(), copy.getGamePhase());
            assertEquals(state.getCurrentPlayer(), copy.getCurrentPlayer());
            assertEquals(1, copy.getHoleCard().getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            assertEquals(hidden(state), hidden(copy));

            if (!copy.getHoleCard().peek().equals(state.getHoleCard().peek())) holeChanged++;
            if (!copy.getDrawDeck().getComponents().equals(state.getDrawDeck().getComponents())) drawDeckChanged++;
        }
        assertTrue("the hole card was never redeterminised", holeChanged > 0);
        assertTrue("the draw deck was never redeterminised", drawDeckChanged > 0);
    }

    @Test
    public void onceTurnedUpTheDealersCardsAreNotRedeterminised() {
        // 6 + 10 = 16: the dealer draws the 3 (19) after the last player stands
        stackDrawDeck(state, "3H");
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertEquals(setOf("6S 10D 3H"), setOf(state.getDealerHand()));
        for (int i = 0; i < 10; i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy(0);
            assertEquals(state.getDealerHand().getComponents(), copy.getDealerHand().getComponents());
            assertEquals(0, copy.getHoleCard().getSize());
            assertArrayEquals(state.chips, copy.chips);
            assertArrayEquals(state.getPlayerResults(), copy.getPlayerResults());
        }
    }
}
