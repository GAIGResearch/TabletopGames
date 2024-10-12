package games.sushigo;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class shuffleTests {

    SGForwardModel fm = new SGForwardModel();
    SGParameters params;
    SGGameState state;


    @Before
    public void setup() {
        params = new SGParameters();
        params.setRandomSeed(4902);
        state = new SGGameState(params, 4);
        fm.setup(state);
    }


    @Test
    public void testRedeterminisationWithNoCardsKnown() {
        // we copy the state of the game, and check that all the other players have shuffled hands
        // and that we have the same one
        SGGameState copy = (SGGameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(8, hand.getSize());
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
        SGGameState copy = (SGGameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(7, hand.getSize());
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
    public void testRedeterminisationWithThreeCardsKnown() {
        do {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.getTurnCounter() < 12);
        SGGameState copy = (SGGameState) state.copy(2);
        for (int i = 0; i < 4; i++) {
            var hand = state.getPlayerHands().get(i);
            assertEquals(5, hand.getSize());
            // iterate over all cards - we now know everything about the hands of all players (the unseen hand is known by elimination)
            for (int j = 0; j < hand.getSize(); j++) {
                assertEquals(copy.getPlayerHands().get(i).get(j), state.getPlayerHands().get(i).get(j));
            }
        }
    }

    @Test
    public void cardsPassedClockwise() {
        SGGameState copy = (SGGameState) state.copy(-1);
        do {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            fm.next(state, availableActions.get(state.getRnd().nextInt(availableActions.size())));
        } while (state.getTurnCounter() < 4);

        // now check that player 0 has the old hand of player 1;
        // and that player 3 has the old hand of player 0

        int commonCount = copy.getPlayerHands().get(1).stream().filter(state.getPlayerHands().get(0)::contains).toArray().length;
        assertEquals(7, commonCount);
        commonCount = copy.getPlayerHands().get(0).stream().filter(state.getPlayerHands().get(3)::contains).toArray().length;
        assertEquals(7, commonCount);

    }

}

