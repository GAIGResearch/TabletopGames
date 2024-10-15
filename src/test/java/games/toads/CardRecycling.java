package games.toads;

import core.actions.AbstractAction;
import games.toads.actions.*;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class CardRecycling {


    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;
    Random rnd;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        params.setParameterValue("discardOption", true);
        params.setParameterValue("useTactics", false);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
        rnd = new Random(933);
    }
    private void playCards(ToadCard... cardsInOrder) {
        for (int i = 0; i < cardsInOrder.length; i++) {
            state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
        }
    }

    @Test
    public void recycleOptionsPresentedToPlayersAtTheStartOfEachBattle() {
        int firstPlayer = 0;
        do {
            assertEquals(firstPlayer, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertTrue(actions.stream().allMatch(a -> a instanceof RecycleCard));
            for (ToadCard  card : state.getPlayerHand(firstPlayer)) {
                assertTrue(actions.contains( new RecycleCard(card)));
            }
            assertTrue(actions.contains(new RecycleCard(null)));

            fm.next(state, new RecycleCard(null));

            assertEquals(1 - firstPlayer, state.getCurrentPlayer());
            actions = fm.computeAvailableActions(state);
            assertTrue(actions.stream().allMatch(a -> a instanceof RecycleCard));
            for (ToadCard  card : state.getPlayerHand(1 - firstPlayer)) {
                assertTrue(actions.contains( new RecycleCard(card)));
            }
            assertTrue(actions.contains(new RecycleCard(null)));

            fm.next(state, new RecycleCard(null));

            firstPlayer = 1 - firstPlayer;

            // now run battle - precise results are not important
            // we have switched off tactics, so just need to trigger four actions
            for (int i = 0; i < 4; i++) {
                List<AbstractAction> battleActions = fm.computeAvailableActions(state);
                fm.next(state, battleActions.get(rnd.nextInt(battleActions.size())));
            }

            if (state.getRoundCounter() == 1 && state.getTurnCounter() == 0) {
                // set first player for next round
                if (state.battlesWon[0][0] - state.battlesWon[0][1] > 0) {
                    firstPlayer = 0;
                } else {
                    firstPlayer = 1;
                }
            }
        } while (state.isNotTerminal());
    }

    @Test
    public void recyclingACardGivesVisibilityOfBottomOfDeck() {
        assertEquals(0, state.getCurrentPlayer());
        ToadCard card = state.getPlayerHand(0).get(0);
        fm.next(state, new RecycleCard(card));

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(4, state.getPlayerHand(0).getSize());
        assertEquals(5, state.getPlayerDeck(0).getSize()); // still 9 cards
        assertFalse(state.getPlayerHand(0).contains(card));
        assertEquals(card, state.getPlayerDeck(0).get(state.getPlayerDeck(0).getSize() - 1));
        for (int i = 0; i < state.getPlayerDeck(0).getSize(); i++) {
            if (i == state.getPlayerDeck(0).getSize() - 1) {
                assertTrue(state.getPlayerDeck(0).isComponentVisible(i, 0));
            } else {
                assertFalse(state.getPlayerDeck(0).isComponentVisible(i, 0));
            }
            assertFalse(state.getPlayerDeck(0).isComponentVisible(i, 1));
        }
    }
}
