package games.toads;

import games.toads.abilities.Assassin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Tactics {


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
    public void trickster() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Six", 6);
        state.hiddenFlankCards[0] = new ToadCard("Six", 6);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3);

        // Trickster should swap the field card with the flank card
        // Score without Trickster functionality would be 1 : 1; with Trickster it is 1 : 0
        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[1][0]);
    }

    @Test
    public void assassinVersusTrickster() {
        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Seven", 7);
        state.hiddenFlankCards[0] = new ToadCard("Assassin", 1, new Assassin());
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3);

        // Trickster should swap the field card with the flank card
        // Score without Assassin functionality would be 2 : 0, with it is 0 : 2
        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerWithOneBonus() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("One", 1);
        state.fieldCards[1] = new ToadCard("Four", 4);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerFieldWithTwoBonusToNotMakeADraw() {
        state.battlesWon[0][1] = 2;
        state.battlesWon[1][1] = 1;
        state.fieldCards[0] = new ToadCard("Berserker", 5);
        state.fieldCards[1] = new ToadCard("Seven", 7);
        state.hiddenFlankCards[0] = new ToadCard("Two", 2);
        state.hiddenFlankCards[1] = new ToadCard("Two", 2);

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(3, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerFlankWithTwoBonusToMakeADraw() {
        state.battlesWon[0][1] = 2;
        state.battlesWon[1][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

        fm._afterAction(state, null);
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void berserkerFlankWithThreeBonus() {
        state.battlesWon[0][1] = 3;
        state.battlesWon[1][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("Berserker", 5);
        state.hiddenFlankCards[1] = new ToadCard("Seven", 7);

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(3, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerWithPushback() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6);
        state.hiddenFlankCards[1] = new ToadCard("Four", 4);

        fm._afterAction(state, null);
        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void iconBearerWithTrickster() {
        state.battlesWon[0][1] = 1;
        state.fieldCards[0] = new ToadCard("Two", 2);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("IconBearer", 6);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3);

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void scout() {
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertFalse(state.playerHands.get(1).isComponentVisible(i, 0));
        state.fieldCards[0] = new ToadCard("Three", 3);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2);
        state.hiddenFlankCards[1] = new ToadCard("Four", 4);

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertTrue(state.playerHands.get(1).isComponentVisible(i, 0));
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsI() {
        state.fieldCards[0] = new ToadCard("Three", 3);
        state.fieldCards[1] = new ToadCard("Two", 2);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3);

        fm._afterAction(state, null);
        for (int i = 0; i < state.playerHands.get(0).getSize(); i++)
            assertFalse(state.playerHands.get(0).isComponentVisible(i, 1));
        for (int i = 0; i < state.playerHands.get(1).getSize(); i++)
            assertTrue(state.playerHands.get(1).isComponentVisible(i, 0));
    }

    @Test
    public void cardMovedWithTricksterDoesNotTriggerTacticsII() {
        state.battlesWon[0][0] = 1;
        state.fieldCards[0] = new ToadCard("Three", 3);
        state.fieldCards[1] = new ToadCard("IconBearer", 6);
        state.hiddenFlankCards[0] = new ToadCard("Scout", 2);
        state.hiddenFlankCards[1] = new ToadCard("Trickster", 3);

        fm._afterAction(state, null);
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }
}
