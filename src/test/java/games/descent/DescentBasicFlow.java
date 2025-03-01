package games.descent;

import core.actions.AbstractAction;
import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static games.descent2e.DescentHelper.figureDeath;
import static org.junit.Assert.*;

public class DescentBasicFlow {

    DescentGameState state;
    DescentForwardModel fm = new DescentForwardModel();
    Random rnd = new Random(492);

    @Before
    public void setup() {
        // a rather cruddy way of ensuring we get the right hero in the right place
        DescentParameters params = new DescentParameters();
        params.setRandomSeed(234);
        state = new DescentGameState(new DescentParameters(), 2);
        fm.setup(state);
    }

    @Test
    public void testEndTurnProcess() {
        // we run through checking that:
        // Each hero has all their actions in turn first
        // then We move through the monsters in order before the round ends
        int lastPlayer, currentPlayer;
        Figure lastFigure, actingFigure;
        int countOfFigures = 0;
        do {
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();

            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            System.out.println("Player " + currentPlayer + " - " + actingFigure + " - " + action.toString());
            fm.next(state, action);

            lastPlayer = currentPlayer;
            lastFigure = actingFigure;
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();
            if (actingFigure.getComponentID() != lastFigure.getComponentID()) {
                countOfFigures++;
                assertNotEquals(lastFigure, actingFigure);
                assertNotEquals(lastFigure.getComponentID(), actingFigure.getComponentID());
                switch (countOfFigures) {
                    case 1:
                        assertEquals(1, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Hero);
                        assertEquals(0, state.getTurnCounter());
                        break;
                    case 2:
                        assertEquals(1, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        break;
                    case 3, 4, 5, 6:
                        assertEquals(0, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        break;
                    case 7:
                        assertEquals(0, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Hero);
                        assertEquals(0, state.getTurnCounter());
                        assertEquals(1, state.getRoundCounter());
                        break;
                    default:
                        throw new AssertionError("Too many figures in the game");
                }
            }

        } while (state.getRoundCounter() == 0);
    }

    @Test
    public void testMonsterRemoved() {
        int lastPlayer, currentPlayer;
        Figure lastFigure, actingFigure;
        int countOfFigures = 0;
        do {
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();

            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            System.out.println("Player " + currentPlayer + " - " + actingFigure + " - " + action.toString());
            fm.next(state, action);

            lastPlayer = currentPlayer;
            lastFigure = actingFigure;
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();
            if (actingFigure.getComponentID() != lastFigure.getComponentID()) {
                countOfFigures++;
                assertNotEquals(lastFigure, actingFigure);
                assertNotEquals(lastFigure.getComponentID(), actingFigure.getComponentID());
                switch (countOfFigures) {
                    case 1:
                        assertEquals(1, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Hero);
                        // Here we now kill off the goblin master
                        figureDeath(state, state.getMonsters().get(0).get(0));
                        assertEquals(0, state.getTurnCounter());
                        break;
                    case 2:
                        assertEquals(1, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Goblin"));
                        break;
                    case 3: // goblins
                        assertEquals(0, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Goblin"));
                        break;
                    case 4, 5: // Barghests
                        assertEquals(0, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Barghest"));
                        break;
                    case 6:
                        assertEquals(0, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Hero);
                        assertEquals(0, state.getTurnCounter());
                        assertEquals(1, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Hero"));
                        break;
                    default:
                        throw new AssertionError("Too many figures in the game");
                }
            }

        } while (state.getRoundCounter() == 0);
    }


    @Test
    public void testMonsterGroupRemoved() {
        int lastPlayer, currentPlayer;
        Figure lastFigure, actingFigure;
        int countOfFigures = 0;
        do {
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();

            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            System.out.println("Player " + currentPlayer + " - " + actingFigure + " - " + action.toString());
            fm.next(state, action);

            lastPlayer = currentPlayer;
            lastFigure = actingFigure;
            currentPlayer = state.getCurrentPlayer();
            actingFigure = state.getActingFigure();
            if (actingFigure.getComponentID() != lastFigure.getComponentID()) {
                countOfFigures++;
                assertNotEquals(lastFigure, actingFigure);
                assertNotEquals(lastFigure.getComponentID(), actingFigure.getComponentID());
                switch (countOfFigures) {
                    case 1:
                        assertEquals(1, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Hero);
                        // Here we now kill off all the goblins
                        figureDeath(state, state.getMonsters().get(0).get(0));
                        figureDeath(state, state.getMonsters().get(0).get(0));
                        figureDeath(state, state.getMonsters().get(0).get(0));
                        assertEquals(0, state.getTurnCounter());
                        break;
                    case 2:
                        assertEquals(1, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Hero);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Barghest"));
                        break;
                    case 3:
                        assertEquals(0, lastPlayer);
                        assertEquals(0, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Monster);
                        assertEquals(1, state.getTurnCounter());
                        assertEquals(0, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Barghest"));
                        break;
                    case 4:
                        assertEquals(0, lastPlayer);
                        assertEquals(1, currentPlayer);
                        assertTrue(lastFigure instanceof Monster);
                        assertTrue(actingFigure instanceof Hero);
                        assertEquals(0, state.getTurnCounter());
                        assertEquals(1, state.getRoundCounter());
                        assertTrue(actingFigure.getName().contains("Hero"));
                        break;
                    default:
                        throw new AssertionError("Too many figures in the game");
                }
            }

        } while (state.getRoundCounter() == 0);
    }

}
