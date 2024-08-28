package games.toads;

import core.actions.AbstractAction;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class TestVisibility {


    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;
    Random rnd;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        params.useTactics = false;
        params.discardOption = false;
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
        rnd = new Random(933);
    }

    private void playCards(ToadCard... cardsInOrder) {
        for (int i = 0; i < cardsInOrder.length; i++) {
            //   state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
        }
    }


    @Test
    public void fieldCardPlayRemovesFromHandImmediately() {

        state.getPlayerHand(0).setVisibilityOfComponent(0, 1, true);
        ToadCard firstCardInHand = state.getPlayerHand(state.getCurrentPlayer()).get(0);
        ToadCard secondCardInHand = state.getPlayerHand(state.getCurrentPlayer()).get(1);

        playCards(
                firstCardInHand,  // Field
                secondCardInHand  // Flank
        );

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(3, state.getPlayerHand(0).getSize());
        for (int i = 0; i < state.getPlayerHand(0).getSize(); i++) {
            assertFalse(state.getPlayerHand(0).getVisibilityForPlayer(i, 1));
        }
    }

    @Test
    public void flankCardPlayDoesNotRemoveFromHandUntilReveal() {

        state.getPlayerHand(0).setVisibilityOfComponent(1, 1, true);
        ToadCard firstCardInHand = state.getPlayerHand(state.getCurrentPlayer()).get(0);
        ToadCard secondCardInHand = state.getPlayerHand(state.getCurrentPlayer()).get(1);

        playCards(
                firstCardInHand,  // Field
                secondCardInHand  // Flank
        );

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(3, state.getPlayerHand(0).getSize());
        for (int i = 0; i < state.getPlayerHand(0).getSize(); i++) {
            if (i == 0)
                assertTrue(state.getPlayerHand(0).getVisibilityForPlayer(i, 1));
            else
                assertFalse(state.getPlayerHand(0).getVisibilityForPlayer(i, 1));
        }

        playCards(
                state.getPlayerHand(1).get(0),
                state.getPlayerHand(1).get(1)
        );

        assertEquals(4, state.getPlayerHand(0).getSize());
        assertEquals(4, state.getPlayerHand(1).getSize());
        for (int i = 0; i < state.getPlayerHand(0).getSize(); i++) {
            assertFalse(state.getPlayerHand(0).getVisibilityForPlayer(i, 1));
        }
    }
}
