package games.dicemonastery.test;


import core.actions.*;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static org.junit.Assert.*;

public class ActionTests {
    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));
    DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
    DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) game.getGameState().getTurnOrder();
    RandomPlayer rnd = new RandomPlayer();

    private void startOfUseMonkPhaseForArea(ActionArea region, Season season) {
        // first place all monks randomly
        do {
            // then Pass until we get to the point required
            while (state.getGamePhase() == USE_MONKS)
                fm.next(state, new Pass());

            do {
                fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
            } while (state.getGamePhase() == PLACE_MONKS);

            // then Pass until we get to the point required
            do {
                fm.next(state, new Pass());
            } while (turnOrder.getCurrentArea() != region);

        } while (turnOrder.getSeason() != season);
    }

    @Test
    public void meadowActionsCorrectSpring() {
        startOfUseMonkPhaseForArea(MEADOW, SPRING);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new SowWheat()));
        assertTrue(fm.computeAvailableActions(state).contains(new Forage()));
        assertTrue(fm.computeAvailableActions(state).contains(new PlaceSkep()));
    }

    @Test
    public void meadowActionsCorrectAutumn() {
        startOfUseMonkPhaseForArea(MEADOW, AUTUMN);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new Forage()));

        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new Forage()));
        assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat()));

        state.moveCube(state.getCurrentPlayer(), SKEP, SUPPLY, MEADOW);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new Forage()));
        assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat()));
        assertTrue(fm.computeAvailableActions(state).contains(new CollectSkep()));

        state.useAP(turnOrder.getActionPointsLeft());
        try {
            assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        } catch (AssertionError ignored) {
            return;
        }
        fail();
    }

    @Test
    public void sowWheat() {
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        state.useAP(-1);
        (new SowWheat()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
    }

    @Test
    public void harvestGrain() {
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        state.useAP(-1);
        (new HarvestWheat()).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(3, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
    }

    @Test
    public void placeSkep() {
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        state.useAP(-1);
        (new PlaceSkep()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
    }

    @Test
    public void collectSkep() {
        state.useAP(-2);
        (new PlaceSkep()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), WAX, STOREROOM));
        (new CollectSkep()).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        assertEquals(3, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(3, state.getResource(state.getCurrentPlayer(), WAX, STOREROOM));
    }

    @Test
    public void forage() {
        state.useAP(-100);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAPES, STOREROOM));
        for (int i = 0; i < 100; i++)
            (new Forage()).execute(state);
        assertEquals(33, state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM), 10);
        assertEquals(33, state.getResource(state.getCurrentPlayer(), GRAPES, STOREROOM), 10);
    }

    @Test
    public void kitchenActionsCorrect() {
        fail("Not yet implemented");
    }

    @Test
    public void bakeBread() {
        fail("Not yet implemented");
    }

    @Test
    public void prepareInk() {
        fail("Not yet implemented");
    }

    @Test
    public void brewBeer() {
        fail("Not yet implemented");
    }

    @Test
    public void brewMead() {
        fail("Not yet implemented");
    }
}
