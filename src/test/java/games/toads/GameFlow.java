package games.toads;

import core.CoreConstants;
import games.toads.abilities.Assassin;
import games.toads.abilities.Bomb;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameFlow {

    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
    }

    @Test
    public void gameInitialisation() {
        assertEquals(5, state.playerDecks.get(0).getSize());
        assertEquals(5, state.playerDecks.get(1).getSize());
        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(0, state.playerDiscards.get(0).getSize());
        assertEquals(0, state.playerDiscards.get(1).getSize());
    }

    @Test
    public void playersPlayCardsInTurn() {
        assertEquals(0, state.getCurrentPlayer());
        // check we have 0 or 1 value 7 cards (as the only possible duplicate)
        assertEquals(0, state.getPlayerHand(0).stream().filter(c -> c.value == 7).count(), 1);
        assertEquals(4, fm.computeAvailableActions(state).size());
        for (ToadCard card : state.getPlayerHand(0)) {
            assertTrue(fm.computeAvailableActions(state).stream().anyMatch(a -> ((PlayFieldCard) a).card == card));
        }
        fm.computeAvailableActions(state).get(0).execute(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, fm.computeAvailableActions(state).size());
        for (ToadCard card : state.getPlayerHand(0)) {
            assertTrue(fm.computeAvailableActions(state).stream().anyMatch(a -> ((PlayFlankCard) a).card == card));
        }
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());

        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(3, state.playerDecks.get(0).getSize());
        assertEquals(5, state.playerDecks.get(1).getSize());
        // then they play two cards
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getRoundCounter());
        assertEquals(1, state.getTurnCounter());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getRoundCounter());
        assertEquals(2, state.getTurnCounter());
        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(3, state.playerDecks.get(0).getSize());
        assertEquals(3, state.playerDecks.get(1).getSize());
    }

    @Test
    public void gameEndsAfterTwoTurnsEach() {
        for (int i = 0; i < 16; i++) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getTurnCounter());
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        for (int i = 0; i < 16; i++) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void scoreUpdatesForVanillaCards() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);

        state.hiddenFlankCards[0] = new ToadCard("Six", 6);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);

        fm._afterAction(state, null);

        assertEquals(1, state.battlesWon[0]);
        assertEquals(0, state.battlesWon[1]);
    }

    @Test
    public void assassinAgainstSeven() {
        state.fieldCards[0] = new ToadCard("Three", 3);
        state.fieldCards[1] = new ToadCard("Five", 5);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 0, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);
        fm._afterAction(state, null);

        assertEquals(1, state.battlesWon[0]);
        assertEquals(1, state.battlesWon[1]);
    }

    @Test
    public void assassinAgainstSix() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Five", 5);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 0, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);
        fm._afterAction(state, null);

        assertEquals(0, state.battlesWon[0]);
        assertEquals(1, state.battlesWon[1]);
    }

    @Test
    public void bombAgainstFourAttack() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Five", 5);
        state.hiddenFlankCards[0] = new ToadCard("Bomb", 0, new Bomb());
        state.hiddenFlankCards[1] = new ToadCard("Four", 4);
        fm._afterAction(state, null);

        assertEquals(0, state.battlesWon[0]);
        assertEquals(1, state.battlesWon[1]);
    }

    @Test
    public void bombAgainstFourDefense() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Five", 5);
        state.hiddenFlankCards[1] = new ToadCard("Bomb", 0, new Bomb());
        state.hiddenFlankCards[0] = new ToadCard("Four", 4);
        fm._afterAction(state, null);

        assertEquals(1, state.battlesWon[0]);
        assertEquals(0, state.battlesWon[1]);
    }

    @Test
    public void bombAgainstSixDefense() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Five", 5);
        state.hiddenFlankCards[1] = new ToadCard("Bomb", 0, new Bomb());
        state.hiddenFlankCards[0] = new ToadCard("Six", 6);
        fm._afterAction(state, null);

        assertEquals(0, state.battlesWon[0]);
        assertEquals(1, state.battlesWon[1]);
    }

    @Test
    public void overcommit() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);
        state.hiddenFlankCards[0] = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);
        fm._afterAction(state, null);

        assertEquals(1, state.battlesWon[0]);
        assertEquals(0, state.battlesWon[1]);
    }

    @Test
    public void pushback() {
        state.battlesWon[1] = 1;
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);
        state.hiddenFlankCards[0] = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);
        fm._afterAction(state, null);

        assertEquals(2, state.battlesWon[0]);
        assertEquals(1, state.battlesWon[1]);
    }

    @Test
    public void redeterminisationShufflesFlankButNotFieldCards() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);
        state.hiddenFlankCards[0] = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);
        ToadGameState copy = (ToadGameState) state.copy(0);
        assertEquals(state.fieldCards[0], copy.fieldCards[0]);
        assertEquals(state.fieldCards[1], copy.fieldCards[1]);
        assertEquals(state.hiddenFlankCards[0], copy.hiddenFlankCards[0]);
        assertNotEquals(state.hiddenFlankCards[1], copy.hiddenFlankCards[1]);
    }
}
