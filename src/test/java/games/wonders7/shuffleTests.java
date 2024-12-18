package games.wonders7;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class shuffleTests {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;


    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4902);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }


    @Test
    public void testRedeterminisationWithNoCardsKnown() {
        // we copy the state of the game, and check that all the other players have shuffled hands
        // and that we have the same one
        Wonders7GameState copy = (Wonders7GameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(7, hand.getSize());
            if (i == 2) {
                // iterate over all cards
                for (int j = 0; j < hand.getSize(); j++) {
                    assertEquals(copy.getPlayerHands().get(i).get(j), state.getPlayerHands().get(i).get(j));
                }
            } else {
                int identicalCount = 0;
                for (int j = 0; j < hand.getSize(); j++) {
                    if (copy.getPlayerHands().get(i).get(j).equals(state.getPlayerHands().get(i).get(j))) {
                        identicalCount++;
                    }
                }
                assertEquals(1, identicalCount, 1); // we'll allow up to 2 cards to be the same per hand; as long as most change
            }
        }

    }

    @Test
    public void testRedeterminisationWithOneCardKnown() {
        // we take one turn for each player, so that hands are passed on, and all players know all the cards in the hand of the player to their left
        do {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.getTurnCounter() < 4);
        Wonders7GameState copy = (Wonders7GameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(6, hand.getSize());
            if (i == 2 || i == 1) {
                // iterate over all cards
                for (int j = 0; j < hand.getSize(); j++) {
                    assertEquals(copy.getPlayerHands().get(i).get(j), state.getPlayerHands().get(i).get(j));
                }
            } else {
                int identicalCount = 0;
                for (int j = 0; j < hand.getSize(); j++) {
                    if (copy.getPlayerHands().get(i).get(j).equals(state.getPlayerHands().get(i).get(j))) {
                        identicalCount++;
                    }
                }
                assertEquals(1, identicalCount, 1); // we'll allow up to 2 cards to be the same per hand; as long as most change
            }
        }
    }


    @Test
    public void testRedeterminisationWithOneCardKnownReverseDirection() {
        // we take one turn for each player, so that hands are passed on, and all players know all the cards in the hand of the player to their left
        // first we move to the second age
        do {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.getRoundCounter() == 0 || state.getPlayerHand(0).getSize() == 7);
        assertEquals(-1, state.getDirection());
        Wonders7GameState copy = (Wonders7GameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(6, hand.getSize());
            if (i == 2 || i == 3) {
                // iterate over all cards
                for (int j = 0; j < hand.getSize(); j++) {
                    assertEquals(copy.getPlayerHands().get(i).get(j), state.getPlayerHands().get(i).get(j));
                }
            } else {
                int identicalCount = 0;
                for (int j = 0; j < hand.getSize(); j++) {
                    if (copy.getPlayerHands().get(i).get(j).equals(state.getPlayerHands().get(i).get(j))) {
                        identicalCount++;
                    }
                }
                assertEquals(1, identicalCount, 1); // we'll allow up to 2 cards to be the same per hand; as long as most change
            }
        }
    }

    @Test
    public void testRedeterminisationWithAllCardsKnown() {
        do {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.getTurnCounter() < 12);
        Wonders7GameState copy = (Wonders7GameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(4, hand.getSize());
            for (int j = 0; j < hand.getSize(); j++) {
                assertEquals(copy.getPlayerHands().get(i).get(j), state.getPlayerHands().get(i).get(j));
            }
        }
    }

    @Test
    public void testPreviousActionsAreRemovedOnRedeterminisation() {
        fm.next(state, fm.computeAvailableActions(state).get(0));
        fm.next(state, fm.computeAvailableActions(state).get(0));
        Wonders7GameState copy = (Wonders7GameState) state.copy(2);
        // the copy should have no actions...
        for (int i = 0; i < 4; i++) {
            assertNull(copy.turnActions[i]);
            if (i < 2) {
                assertNotNull(state.turnActions[i]);
            } else {
                assertNull(state.turnActions[i]);
            }
        }
        do {
            // we then process the game on the copy, and check that each player plays one card
            List<AbstractAction> availableActionsCopy = fm.computeAvailableActions(copy);
            fm.next(copy, availableActionsCopy.get(copy.getRnd().nextInt(availableActionsCopy.size())));
        } while (copy.playerHands.get(2).getSize() == 7);
        // The turn counter is not reset (as it really should be on the copy...but this doesn't leak any information)
        assertEquals(6, copy.getTurnCounter());
        for (int i = 0; i < 4; i++) {
            assertNull(copy.turnActions[i]);
            assertEquals(6, copy.getPlayerHands().get(i).getSize());
        }

    }
}

