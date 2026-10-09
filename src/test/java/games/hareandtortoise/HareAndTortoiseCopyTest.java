package games.hareandtortoise;

import games.hareandtortoise.components.HareCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static games.hareandtortoise.components.HareCard.Type.*;
import static org.junit.Assert.*;

public class HareAndTortoiseCopyTest {

    HareAndTortoiseGameState state;

    @Before
    public void setup() {
        state = new HareAndTortoiseGameState(new HareAndTortoiseParameters(), 4);
        new HareAndTortoiseForwardModel().setup(state);
        place(state, 0, 7, 37, 3);
        state.lettuceToChew[0] = true;
        place(state, 1, 12, 20, 1);
        state.missNextTurn[1] = true;
        putHome(state, 2, 1);
        state.nUnseenHareCards = 9;
    }

    @Test
    public void faithfulCopyEqualsTheOriginalWithTheSameHash() {
        HareAndTortoiseGameState copy = (HareAndTortoiseGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(state.getHareDeck().getComponents(), copy.getHareDeck().getComponents());
    }

    @Test
    public void changingTheCopyLeavesTheOriginalUnchanged() {
        HareAndTortoiseGameState copy = (HareAndTortoiseGameState) state.copy();
        copy.squares[0] = 9;
        copy.carrots[1] = 0;
        copy.lettuces[0] = 2;
        copy.lettuceToChew[0] = false;
        copy.missNextTurn[1] = false;
        copy.finishPositions[3] = 2;
        copy.hareDeck.draw();
        copy.nUnseenHareCards = 8;
        assertNotEquals(state, copy);

        assertEquals(7, state.getSquare(0));
        assertEquals(20, state.getCarrots(1));
        assertEquals(3, state.getLettuces(0));
        assertTrue(state.hasLettuceToChew(0));
        assertTrue(state.missesNextTurn(1));
        assertEquals(0, state.getFinishPosition(3));
        assertEquals(12, state.getHareDeck().getSize());
        assertEquals(9, state.getNUnseenHareCards());
    }

    @Test
    public void eachFieldTakesPartInEquality() {
        HareAndTortoiseGameState copy = (HareAndTortoiseGameState) state.copy();
        copy.squares[3] = 1;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.carrots[3] = 64;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.lettuces[3] = 2;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.lettuceToChew[3] = true;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.missNextTurn[3] = true;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.finishPositions[3] = 2;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.nUnseenHareCards = 0;
        assertNotEquals(state, copy);
        copy = (HareAndTortoiseGameState) state.copy();
        copy.hareDeck.addToBottom(copy.hareDeck.draw());
        // the deck is shuffled: moving the top card to the bottom changes it unless all 12 cards are the same type
        assertNotEquals(state, copy);
    }

    // ---------------------------------------------------------------- redeterminisation of the hare cards

    private HareAndTortoiseGameState copyFor(int player) {
        return (HareAndTortoiseGameState) state.copy(player);
    }

    @Test
    public void aPlayersCopyShufflesOnlyTheUnseenTopCardsAmongThemselves() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION, LAST_TURN_FREE, DRAW_OR_DISCARD, LEAP_AHEAD_ONE_POSITION,
                NEXT_CARROT_SQUARE, PREVIOUS_CARROT_SQUARE);
        state.nUnseenHareCards = 6;
        List<HareCard.Type> original = hareTypes(state);
        List<Set<HareCard.Type>> seenAt = new ArrayList<>();
        for (int i = 0; i < 6; i++) seenAt.add(new HashSet<>());

        for (int n = 0; n < 30; n++) {
            HareAndTortoiseGameState copy = copyFor(1);
            List<HareCard.Type> types = hareTypes(copy);
            assertEquals(12, types.size());
            assertEquals(6, copy.getNUnseenHareCards());
            // the six seen cards keep their order
            assertEquals(original.subList(6, 12), types.subList(6, 12));
            // the unseen six are the same six (distinct) cards
            assertEquals(new HashSet<>(original.subList(0, 6)), new HashSet<>(types.subList(0, 6)));
            for (int i = 0; i < 6; i++) seenAt.get(i).add(types.get(i));
        }
        // successive copies differ only in the redeterminisation generator: every unseen place must vary
        for (int i = 0; i < 6; i++)
            assertTrue("place " + i + " never changes", seenAt.get(i).size() > 1);
        assertEquals(original, hareTypes(state));
    }

    @Test
    public void aPlayersCopyShufflesTheWholePileWhileNothingHasBeenSeen() {
        state.nUnseenHareCards = 12;
        Set<List<HareCard.Type>> orders = new HashSet<>();
        for (int n = 0; n < 5; n++)
            orders.add(hareTypes(copyFor(0)));
        assertTrue(orders.size() > 1);
    }

    @Test
    public void aPlayersCopyKeepsTheOrderOnceEveryCardHasBeenSeen() {
        state.nUnseenHareCards = 0;
        for (int n = 0; n < 10; n++) {
            HareAndTortoiseGameState copy = copyFor(1);
            assertEquals(hareTypes(state), hareTypes(copy));
            assertEquals(state, copy);
        }
    }

    @Test
    public void theFaithfulCopyNeverReordersTheUnseenCards() {
        state.nUnseenHareCards = 12;
        for (int n = 0; n < 10; n++)
            assertEquals(hareTypes(state), hareTypes(copyFor(-1)));
    }

    @Test
    public void aPlayersCopyIsFaithfulWhenNotPartiallyObservable() {
        state.getCoreGameParameters().partialObservable = false;
        state.nUnseenHareCards = 12;
        for (int n = 0; n < 10; n++)
            assertEquals(hareTypes(state), hareTypes(copyFor(1)));
    }
}
