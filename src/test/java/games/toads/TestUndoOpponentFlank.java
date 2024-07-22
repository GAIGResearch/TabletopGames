package games.toads;

import core.actions.AbstractAction;
import core.components.Deck;
import games.toads.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestUndoOpponentFlank {

    ToadGameState state;
    ToadForwardModel fm;
    ToadParameters params;
    Random rnd = new Random(931);

    @Before
    public void setup() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
        rnd = new Random(933);
    }

    private void playCards(ToadCard... cardsInOrder) {
        for (int i = 0; i < cardsInOrder.length; i++) {
           // state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
        }
    }

    private void moveStateForwardToFirstDefenderMove()  {
        playCards(
                state.getPlayerHand(0).get(0),
                state.getPlayerHand(0).get(1)
        );
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void basicInstantiation() {
        moveStateForwardToFirstDefenderMove();
        ToadCard flankCard = state.getHiddenFlankCard(0);
        assertEquals(4, state.getPlayerHand(0).getSize());
        Deck<ToadCard> p0Hand = state.getPlayerHand(0).copy();

        assertEquals(1, state.getCurrentPlayer());
        UndoOpponentFlank undo = new UndoOpponentFlank(state);
        assertEquals(undo, state.getActionsInProgress().get(0));
        assertNull(state.getHiddenFlankCard(0));
        assertEquals(5, state.getPlayerHand(0).getSize());
        assertTrue(state.getPlayerHand(0).contains(flankCard));
        assertEquals(0, state.getCurrentPlayer());

        List<AbstractAction> actions = fm._computeAvailableActions(state);
        assertEquals(5, actions.size());
        p0Hand.add(flankCard);
        for (AbstractAction action : actions) {
            assertTrue(action instanceof PlayFlankCard);
            assertTrue(p0Hand.contains(((PlayFlankCard) action).card));
        }

        fm.next(state, actions.get(2));
        assertTrue(state.getActionsInProgress().isEmpty());
        assertEquals(1, state.getCurrentPlayer());
        assertNotNull(state.getHiddenFlankCard(0));
    }

}
