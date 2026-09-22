package games.gofish;

import core.actions.AbstractAction;
import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * The actions offered, and GoFishAsk as a value.
 */
public class GoFishActionsTest {

    GoFishGameState state;
    GoFishForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 17);
        fm = new GoFishForwardModel();
    }

    @Test
    public void oneAskPerOtherPlayerAndRankHeld() {
        giveHand(state, 0, card("5H"), card("5S"), card("KD"), card("9C"));
        giveHand(state, 1, card("2H"));
        giveHand(state, 2, card("7D"), card("8D"));
        // 2 other players x 3 distinct ranks held (5, K, 9) = 6; nothing for P0 itself, nor for a rank P0 lacks (2, 7, 8)
        Set<AbstractAction> expected = Set.of(
                new GoFishAsk(1, 5), new GoFishAsk(1, 13), new GoFishAsk(1, 9),
                new GoFishAsk(2, 5), new GoFishAsk(2, 13), new GoFishAsk(2, 9));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(expected, new HashSet<>(actions));
        assertEquals("each ask offered once (two 5s give one ask per target)", 6, actions.size());
    }

    @Test
    public void aHandBuiltAskEqualsTheGeneratedOne() {
        giveHand(state, 0, card("QH"));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        GoFishAsk built = new GoFishAsk(2, 12);
        int idx = actions.indexOf(built);
        assertTrue(idx >= 0);
        assertEquals(actions.get(idx), built);
        assertEquals(actions.get(idx).hashCode(), built.hashCode());
        assertSame("immutable: copy() returns this", built, built.copy());
        assertEquals(2, built.targetPlayer);
        assertEquals(12, built.rank);
        assertNotEquals(new GoFishAsk(1, 12), built);
        assertNotEquals(new GoFishAsk(2, 11), built);
    }

    @Test
    public void executingAnAskDoesNotChangeTheAction() {
        // value-based and immutable: the same action object can be applied to two states and still equals a fresh one
        giveHand(state, 0, card("5H"), card("KD"));
        giveHand(state, 1, card("5S"), card("9C"));
        GoFishAsk ask = new GoFishAsk(1, 5);
        GoFishGameState copy = (GoFishGameState) state.copy();
        fm.next(state, ask);
        assertEquals(new GoFishAsk(1, 5), ask);
        assertEquals(new GoFishAsk(1, 5).hashCode(), ask.hashCode());
        fm.next(copy, ask);
        assertEquals(state, copy);
    }
}
