package games.president;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * Full and per-player copies of a mid-deal position and of a position in the exchange.
 */
public class PresidentCopyTest {

    PresidentForwardModel fm;
    PresidentGameState state;

    @Before
    public void setup() {
        // 5 players: player 3 is out (2 points); player 1 played a pair of 9s on a pair of 4s, player 2 passed,
        // player 4 to act; the discard pile holds the cards arranged out of the hands
        fm = new PresidentForwardModel();
        state = newState(5, 51, fm);
        arrangeOut(state, 3);
        state.playerScores[3] = 2;
        arrangeTrick(state, 1, 2, 1, 4, "4H", "4D", "9C", "9S");
        assertTrue(state.getDiscardPile().getSize() > 0);
        assertAllCardsPresent(state);
    }

    @Test
    public void aFullCopyIsEqualAndIndependent() {
        PresidentGameState copy = (PresidentGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // change the copy: move a card from player 0's hand to the play pile, count a pass, someone out and a score
        FrenchCard c = copy.getPlayerHand(0).get(0);
        copy.getPlayerHand(0).remove(c);
        copy.getPlayPile().add(c);
        copy.setPassesInRow(2);
        copy.finishingOrder.add(0);
        copy.playerScores[0] = 1;
        assertNotEquals(state, copy);

        assertTrue(state.getPlayerHand(0).contains(c));
        assertEquals(4, state.getPlayPile().getSize());
        assertEquals(1, state.getPassesInRow());
        assertEquals(List.of(3), state.getFinishingOrder());
        assertEquals(0, state.getPlayerScore(0));
        assertAllCardsPresent(state);
    }

    @Test
    public void copiesDifferingInOneCounterAreNotEqual() {
        PresidentGameState copy = (PresidentGameState) state.copy();
        copy.setLastPlayer(0);
        assertNotEquals(state, copy);
        copy = (PresidentGameState) state.copy();
        copy.setSetSize(1);
        assertNotEquals(state, copy);
    }

    /**
     * 4 players, exchangeCards 2, EXCHANGE phase: the President (2) has one card still to give to the Scum (0).
     * The hands are as dealt, except the Scum's, cut to three cards (the rest to the discard pile).
     */
    private PresidentGameState exchangePosition() {
        PresidentGameState s = newState(multiDealParams(11, 2), 4, 52, fm);
        giveHand(s, 0, "3H", "4D", "5C");
        arrangeExchange(s, 2, 0, 1);
        assertAllCardsPresent(s);
        return s;
    }

    @Test
    public void aCopyInTheExchangeKeepsTheExchangeAndDiffersWithIt() {
        PresidentGameState ex = exchangePosition();
        PresidentGameState copy = (PresidentGameState) ex.copy();
        assertEquals(ex, copy);
        assertEquals(ex.hashCode(), copy.hashCode());
        assertEquals(PresidentGameState.Phase.EXCHANGE, copy.getGamePhase());
        assertEquals(0, copy.getScum());
        assertEquals(1, copy.getCardsToGive());

        copy.scum = 3;
        assertNotEquals(ex, copy);
        copy = (PresidentGameState) ex.copy();
        copy.cardsToGive = 2;
        assertNotEquals(ex, copy);
    }

    @Test
    public void aPlayersCopyInTheExchangeKeepsTheExchange() {
        PresidentGameState ex = exchangePosition();
        int changed = 0;
        for (int i = 0; i < 10; i++) {
            // the President's view
            PresidentGameState copy = (PresidentGameState) ex.copy(2);
            assertEquals(ex.getPlayerHand(2).getComponents(), copy.getPlayerHand(2).getComponents());
            for (int p = 0; p < 4; p++)
                assertEquals(ex.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
            if (!ex.getPlayerHand(0).getComponents().equals(copy.getPlayerHand(0).getComponents())) changed++;
            assertEquals(PresidentGameState.Phase.EXCHANGE, copy.getGamePhase());
            assertEquals(0, copy.getScum());
            assertEquals(1, copy.getCardsToGive());
            assertEquals(2, copy.getCurrentPlayer());
            assertEquals(ex.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
            assertAllCardsPresent(copy);
        }
        assertTrue("the Scum's hand was never reshuffled in the President's copy", changed > 0);
    }

    @Test
    public void aPlayersCopyKeepsWhatThePlayerCanSee() {
        Set<FrenchCard> hiddenFromZero = new HashSet<>();
        for (int p = 1; p < 5; p++)
            hiddenFromZero.addAll(state.getPlayerHand(p).getComponents());

        int changed = 0;
        for (int i = 0; i < 10; i++) {
            PresidentGameState copy = (PresidentGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            Set<FrenchCard> copyHidden = new HashSet<>();
            for (int p = 1; p < 5; p++) {
                assertEquals(state.getPlayerHand(p).getSize(), copy.getPlayerHand(p).getSize());
                copyHidden.addAll(copy.getPlayerHand(p).getComponents());
                if (!state.getPlayerHand(p).getComponents().equals(copy.getPlayerHand(p).getComponents()))
                    changed++;
            }
            // the same cards are hidden from player 0, only redistributed
            assertEquals(hiddenFromZero, copyHidden);
            assertEquals(0, copy.getPlayerHand(3).getSize());
            assertEquals(state.getPlayPile().getComponents(), copy.getPlayPile().getComponents());
            assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
            assertEquals(state.getLastPlayer(), copy.getLastPlayer());
            assertEquals(state.getSetSize(), copy.getSetSize());
            assertEquals(state.getPassesInRow(), copy.getPassesInRow());
            assertEquals(state.getFinishingOrder(), copy.getFinishingOrder());
            assertEquals(state.getPlayerScore(3), copy.getPlayerScore(3));
            assertAllCardsPresent(copy);
        }
        assertTrue("the other hands were never reshuffled", changed > 0);
    }
}
