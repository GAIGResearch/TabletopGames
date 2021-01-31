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
        for (int i = 0; i < 100; i++)
            (new Forage()).execute(state);
        assertEquals(50, state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM), 15);
    }

    @Test
    public void kitchenActionsCorrect() {
        startOfUseMonkPhaseForArea(KITCHEN, SPRING);

        while (state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), GRAIN, STOREROOM, SUPPLY);
        while (state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), HONEY, STOREROOM, SUPPLY);
        while (state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), PIGMENT, STOREROOM, SUPPLY);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));

        state.useAP(turnOrder.getActionPointsLeft() - 1);
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, STOREROOM);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BakeBread()));

        state.useAP(-1);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BakeBread()));
        assertTrue(fm.computeAvailableActions(state).contains(new BrewBeer()));

        state.moveCube(state.getCurrentPlayer(), HONEY, SUPPLY, STOREROOM);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new BrewMead()));

        state.moveCube(state.getCurrentPlayer(), PIGMENT, SUPPLY, STOREROOM);
        assertEquals(5, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareInk()));

        state.useAP(1);
        assertEquals(2, fm.computeAvailableActions(state).size());
    }

    @Test
    public void bakeBread() {
        state.useAP(-1);
        // Has 2 Grain in STOREROOM at setup
        (new BakeBread()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        assertEquals(4, state.getResource(state.getCurrentPlayer(), BREAD, STOREROOM));
    }

    @Test
    public void prepareInk() {
        state.useAP(-1);
        try {
            (new PrepareInk()).execute(state);
            fail("Should throw exception");
        } catch (IllegalArgumentException error) {
           // expected!
        }
        state.useAP(-1);
        state.moveCube(state.getCurrentPlayer(), PIGMENT, SUPPLY, STOREROOM);
        (new PrepareInk()).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), INK, STOREROOM));
    }

    @Test
    public void brewBeer() {
        state.useAP(-2);
        // Has 2 Grain in STOREROOM at setup
        (new BrewBeer()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), BEER, STOREROOM));
    }

    @Test
    public void brewMead() {
        state.useAP(-2);
        // Has 2 Honey in STOREROOM at setup
        (new BrewMead()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), MEAD, STOREROOM));
    }


    @Test
    public void workshopActionsCorrect() {
        startOfUseMonkPhaseForArea(WORKSHOP, SPRING);

        state.useAP(turnOrder.getActionPointsLeft() - 1);

        while (state.getResource(state.getCurrentPlayer(), PIGMENT, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), PIGMENT, STOREROOM, SUPPLY);
        while (state.getResource(state.getCurrentPlayer(), WAX, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), WAX, STOREROOM, SUPPLY);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new WeaveSkep()));

        state.moveCube(state.getCurrentPlayer(), PIGMENT, SUPPLY, STOREROOM);
        assertEquals(2, fm.computeAvailableActions(state).size());

        state.useAP(-1);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareInk()));

        state.moveCube(state.getCurrentPlayer(), WAX, SUPPLY, STOREROOM);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new MakeCandle()));

        state.moveCube(state.getCurrentPlayer(), CALF_SKIN, SUPPLY, STOREROOM);
        assertEquals(5, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareVellum()));

        state.useAP(1);
        assertEquals(2, fm.computeAvailableActions(state).size());
    }

    @Test
    public void weaveSkep() {
        state.useAP(-1);
        assertEquals(2, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        (new WeaveSkep()).execute(state);
        assertEquals(3, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
    }

    @Test
    public void prepareVellum() {
        state.useAP(-2);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), CALF_SKIN, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), VELLUM, STOREROOM));
        try {
            (new PrepareVellum()).execute(state);
            fail("Should not succeed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        state.useAP(-2);
        state.addResource(state.getCurrentPlayer(), CALF_SKIN, 1);
        (new PrepareVellum()).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), CALF_SKIN, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), VELLUM, STOREROOM));
    }

    @Test
    public void makeCandle() {
        int player = state.getCurrentPlayer();
        state.useAP(-1);
        assertEquals(2, state.getResource(player, WAX, STOREROOM));
        assertEquals(0, state.getResource(player, CANDLE, STOREROOM));
        try {
            (new MakeCandle()).execute(state);
            fail("Should not succeed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals(1, turnOrder.getActionPointsLeft());
        state.useAP(-1);
        fm.next(state, (new MakeCandle()));
        assertEquals(1, state.getResource(player, WAX, STOREROOM));
        assertEquals(1, state.getResource(player, CANDLE, STOREROOM));
    }

    @Test
    public void gatehouseActionsCorrect() {
        startOfUseMonkPhaseForArea(GATEHOUSE, SPRING);

        state.useAP(turnOrder.getActionPointsLeft() - 1);

        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BegForAlms()));
        assertTrue(fm.computeAvailableActions(state).contains(new VisitMarket()));

        state.useAP(-1);
        assertEquals(3, fm.computeAvailableActions(state).size());

        state.useAP(-1);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new HireNovice()));
    }

    @Test
    public void begForAlms() {
        int player = state.getCurrentPlayer();
        state.useAP(-1);
        assertEquals(6, state.getResource(player, SHILLINGS, STOREROOM));
        fm.next(state, (new BegForAlms()));
        assertEquals(7, state.getResource(player, SHILLINGS, STOREROOM));
        assertEquals(0, turnOrder.getActionPointsLeft());
    }

    @Test
    public void visitMarketToBuy() {
        state.addResource(state.getCurrentPlayer(), BREAD, -2);
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -4); // should leave 2 over
        VisitMarket visit = new VisitMarket();
        visit._execute(state);
        assertEquals(visit, state.currentActionInProgress());
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Buy(BREAD, 2), fm.computeAvailableActions(state).get(0));

        int player = state.getCurrentPlayer();
        state.addResource(state.getCurrentPlayer(), SHILLINGS, 1);
        assertEquals(3, fm.computeAvailableActions(state).size());

        fm.next(state, (new Buy(CALF_SKIN, 3)));
        assertTrue(visit.executionComplete(state));
        assertEquals(1, state.getResource(player, CALF_SKIN, STOREROOM));
        assertEquals(0, state.getResource(player, SHILLINGS, STOREROOM));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void visitMarketToSell() {
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -5); // 1 left - not enough to buy anything
        state.useAP(-1);
        VisitMarket visit = new VisitMarket();
        int player = state.getCurrentPlayer();
        fm.next(state, visit);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Sell(BREAD, 1), fm.computeAvailableActions(state).get(0));
        assertEquals(player, state.getCurrentPlayer());
        state.addResource(player, BEER, 1);
        state.addResource(player, MEAD, 1);
        assertEquals(3, fm.computeAvailableActions(state).size());
        Sell action = (Sell) fm.computeAvailableActions(state).get(1);

        fm.next(state, action);
        assertTrue(visit.executionComplete(state));
        assertEquals(0, state.getResource(player, action.resource, STOREROOM));
        assertEquals(1 + action.price, state.getResource(player, SHILLINGS, STOREROOM));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void visitMarketToDoNothing() {
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -5); // 1 left - not enough to buy anything
        state.addResource(state.getCurrentPlayer(), BREAD, -2);
        state.useAP(-1);

        VisitMarket visit = new VisitMarket();
        int player = state.getCurrentPlayer();
        fm.next(state, visit);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertTrue(visit.executionComplete(state));
        assertFalse(state.isActionInProgress());
    }

}
