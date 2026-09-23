package games.golfsix;

import core.components.FrenchCard;
import games.golfsix.actions.TurnUp;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies (-1: faithful) and redeterminised copies: face-up cards, the discard pile and a card the observer
 * can see stay; face-down cards (the observer's own too), the draw deck and a card someone else drew from the draw
 * deck are reshuffled.
 */
public class GolfSixCopyTest {

    GolfSixGameState state;
    GolfSixForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 57);
        fm = new GolfSixForwardModel();
        skipReveal(state);
        faceUp(state, 1, 4);
    }

    @Test
    public void aFaithfulCopyIsEqualAndIndependent() {
        GolfSixGameState copy = (GolfSixGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // the copy's grid is a separate object: turning a card up in it leaves the original alone
        faceUp(copy, 0, 5);
        assertEquals("UUDDDD", faceUpPattern(state, 0));
        assertNotEquals("a difference in face-up status alone makes the states unequal", state, copy);
    }

    @Test
    public void aFaithfulCopyIsIndependentOfPlay() {
        GolfSixParameters params = new GolfSixParameters();
        params.setRandomSeed(57);
        GolfSixGameState fresh = newState(params, 3);
        GolfSixGameState copy = (GolfSixGameState) fresh.copy();
        fm.next(copy, new TurnUp(2));
        assertEquals("DDDDDD", faceUpPattern(fresh, 0));
        assertEquals("DDUDDD", faceUpPattern(copy, 0));
        assertNotEquals(fresh, copy);
    }

    @Test
    public void aRedeterminisedCopyKeepsWhatTheObserverCanSee() {
        // player 0 (the current player) has drawn a card from the draw deck, so only player 0 knows it
        setDrawnCard(state, "QD", false);
        checkRedeterminisation(0, true);
    }

    @Test
    public void anotherPlayersCardFromTheDrawDeckIsReshuffled() {
        setDrawnCard(state, "QD", false);
        checkRedeterminisation(1, false);
    }

    @Test
    public void aCardDrawnFromTheDiscardPileIsKnownToAll() {
        setDrawnCard(state, "QD", true);
        checkRedeterminisation(1, true);
    }

    /**
     * Over 30 redeterminised copies for the observer: every visible card and the discard pile are kept, deck sizes
     * are kept, 52 cards are present; some copy changes a face-down card of the observer's own grid, one of another
     * grid, and the draw deck; the drawn card is always kept if drawnCardKnown, else changes in some copy.
     */
    private void checkRedeterminisation(int observer, boolean drawnCardKnown) {
        boolean ownFaceDownChanged = false, otherFaceDownChanged = false, drawDeckChanged = false,
                drawnCardChanged = false;
        List<FrenchCard> discards = new ArrayList<>(state.getDiscardPile().getComponents());
        for (int i = 0; i < 30; i++) {
            GolfSixGameState copy = (GolfSixGameState) state.copy(observer);
            assertAllCardsPresent(copy);
            assertEquals(discards, copy.getDiscardPile().getComponents());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            for (int p = 0; p < 3; p++) {
                assertEquals(faceUpPattern(state, p), faceUpPattern(copy, p));
                for (int pos = 0; pos < 6; pos++) {
                    FrenchCard original = state.getGrid(p).get(pos), copied = copy.getGrid(p).get(pos);
                    if (state.isFaceUp(p, pos))
                        assertEquals("face-up card kept, player " + p + " position " + pos, original, copied);
                    else if (!original.equals(copied)) {
                        if (p == observer) ownFaceDownChanged = true;
                        else otherFaceDownChanged = true;
                    }
                }
            }
            if (!state.getDrawDeck().getComponents().equals(copy.getDrawDeck().getComponents()))
                drawDeckChanged = true;
            if (drawnCardKnown)
                assertEquals(card("QD"), copy.getDrawnCard());
            else if (!card("QD").equals(copy.getDrawnCard()))
                drawnCardChanged = true;
        }
        assertTrue("observer's own face-down cards reshuffled", ownFaceDownChanged);
        assertTrue("other face-down cards reshuffled", otherFaceDownChanged);
        assertTrue("draw deck reshuffled", drawDeckChanged);
        if (!drawnCardKnown)
            assertTrue("another player's drawn card reshuffled", drawnCardChanged);
    }
}
