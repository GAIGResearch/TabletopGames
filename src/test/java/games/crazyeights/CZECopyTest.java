package games.crazyeights;

import core.components.FrenchCard;
import games.crazyeights.actions.Pass;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class CZECopyTest {

    CZEGameState state;
    CZEForwardModel fm;

    @Before
    public void setup() {
        CZEParameters params = new CZEParameters();
        params.setRandomSeed(42);
        state = new CZEGameState(params, 3);
        fm = new CZEForwardModel();
        fm.setup(state);
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        CZEGameState copy = (CZEGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        int originalHash = state.hashCode();
        fm.next(copy, fm.computeAvailableActions(copy).get(0));
        assertEquals(originalHash, state.hashCode());
        assertNotEquals(state, copy);
    }

    private Set<FrenchCard> hiddenFromPlayerZero(CZEGameState s) {
        Set<FrenchCard> cards = new HashSet<>(s.getDrawDeck().getComponents());
        for (int p = 1; p < s.getNPlayers(); p++)
            cards.addAll(s.getPlayerHands().get(p).getComponents());
        return cards;
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerCanSeeAndShufflesTheRest() {
        int handsChanged = 0;
        for (int i = 0; i < 20; i++) {
            CZEGameState copy = (CZEGameState) state.copy(0);
            assertEquals(state.getPlayerHands().get(0).getComponents(), copy.getPlayerHands().get(0).getComponents());
            assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
            assertEquals(state.getCurrentSuit(), copy.getCurrentSuit());
            for (int p = 1; p < 3; p++)
                assertEquals(state.getPlayerHands().get(p).getSize(), copy.getPlayerHands().get(p).getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            assertEquals(hiddenFromPlayerZero(state), hiddenFromPlayerZero(copy));

            if (!copy.getPlayerHands().get(1).getComponents().equals(state.getPlayerHands().get(1).getComponents()))
                handsChanged++;
        }
        assertTrue("player 1's hand was never redeterminised", handsChanged > 0);
    }

    @Test
    public void passCountIsCopiedAndRedeterminisedAndPartOfEquality() {
        // player 0 cannot play on the Five of Hearts and there is nothing to draw, so passes legally
        CZETestUtils.setTopDiscard(state, CZETestUtils.card("5H"));
        CZETestUtils.giveHand(state, 0, CZETestUtils.card("9C"), CZETestUtils.card("KS"));
        CZETestUtils.leaveNothingToDraw(state, 1);
        fm.next(state, new Pass(0));
        assertEquals(1, state.getConsecutivePasses());

        CZEGameState copy = (CZEGameState) state.copy();
        assertEquals(1, copy.getConsecutivePasses());
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(1, ((CZEGameState) state.copy(2)).getConsecutivePasses());

        copy.setConsecutivePasses(0);
        assertNotEquals(state, copy);
        assertEquals(1, state.getConsecutivePasses());
    }
}
