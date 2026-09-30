package games.hareandtortoise;

import org.junit.Before;
import org.junit.Test;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
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
}
