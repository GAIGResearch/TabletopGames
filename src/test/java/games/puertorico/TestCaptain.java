package games.puertorico;

import core.actions.*;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.Ship;
import games.puertorico.roles.DiscardPhase;
import org.junit.*;

import java.util.*;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;
import static games.puertorico.PuertoRicoConstants.Crop.*;
import static org.junit.Assert.*;

public class TestCaptain {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void testCaptainHasOptionOfAvailableShips() {
        state.getPlayerBoard(0).harvest(INDIGO, 3);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new ShipCargo(INDIGO, 0, 3)));
        assertTrue(actions.contains(new ShipCargo(INDIGO, 1, 3)));
        assertTrue(actions.contains(new ShipCargo(INDIGO, 2, 3)));

        state.getShip(0).load(INDIGO, 4); // almost the first ship
        state.getPlayerBoard(0).harvest(CORN, 1);
        actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new ShipCargo(INDIGO, 0, 1)));
        assertTrue(actions.contains(new ShipCargo(CORN, 1, 1)));
        assertTrue(actions.contains(new ShipCargo(CORN, 2, 1)));

        state.getShip(0).load(INDIGO, 1); // completely load the first ship
        state.getShip(2).load(COFFEE, 1);
        actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new ShipCargo(CORN, 1, 1), actions.get(0));
    }

    @Test
    public void testAllPlayersMustSellEverythingIfPossible() {
        state.getPlayerBoard(0).harvest(INDIGO, 2);
        state.getPlayerBoard(0).harvest(CORN, 2);
        state.getPlayerBoard(1).harvest(COFFEE, 2);
        state.getPlayerBoard(2).harvest(CORN, 2);
        state.getPlayerBoard(2).harvest(INDIGO, 1);
        state.getPlayerBoard(2).harvest(SUGAR, 1);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        fm.next(state, fm.computeAvailableActions(state).get(0));  // ships INDIGO/CORN
        assertEquals(2, state.getPlayerBoard(0).getVP());
        fm.next(state, fm.computeAvailableActions(state).get(0));  // ships COFFEE
        assertEquals(0, state.getStoresOf(1, COFFEE));
        fm.next(state, fm.computeAvailableActions(state).get(0));  // ships CORN, OR INDIGO, OR SUGAR
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof ShipCargo));
        fm.next(state, fm.computeAvailableActions(state).get(0));  // ships INDIGO/CORN
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof ShipCargo));
        fm.next(state, fm.computeAvailableActions(state).get(0));  // ships CORN, OR INDIGO, OR SUGAR
        assertEquals(5, state.getPlayerBoard(0).getVP());
        assertEquals(0, state.getStoresOf(0, INDIGO));
        assertEquals(0, state.getStoresOf(0, CORN));
    }

    @Test
    public void testCaptainGainsExtraVPOnTheirLastShipmentOnly() {
        state.getPlayerBoard(0).harvest(INDIGO, 2);
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        int tobaccoStart = state.getSupplyOf(TOBACCO);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertTrue(fm.computeAvailableActions(state).contains(new ShipCargo(TOBACCO, 0, 2)));
        fm.next(state, new ShipCargo(TOBACCO, 0, 2));
        assertEquals(2, state.getGameScore(0), 0.01);
        assertEquals(0, state.getStoresOf(0, TOBACCO));
        assertEquals(TOBACCO, state.getShip(0).getCurrentCargo());
        assertEquals(2, state.getShip(0).capacity - state.getShip(0).getAvailableCapacity());
        assertEquals(tobaccoStart, state.getSupplyOf(TOBACCO));
        while (state.getCurrentPlayer() != 0) {
            // we should come back to this player for their second delivery
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertTrue(fm.computeAvailableActions(state).contains(new ShipCargo(INDIGO, 2, 2)));
        fm.next(state, new ShipCargo(INDIGO, 2, 2));
        assertEquals(5, state.getGameScore(0), 0.01);
    }

    @Test
    public void activeHarbourGivesBonusVPPerDelivery() {
        state.build(1, HARBOUR);
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        state.getPlayerBoard(1).harvest(TOBACCO, 2);
        int tobaccoStart = state.getSupplyOf(TOBACCO);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertEquals(1, state.getCurrentPlayer());
        double startScore = state.getGameScore(1);
        fm.next(state, new ShipCargo(TOBACCO, 0, 2));
        assertEquals(startScore + 3, state.getGameScore(1), 0.01);
        assertEquals(0, state.getStoresOf(1, TOBACCO));
        assertEquals(TOBACCO, state.getShip(0).getCurrentCargo());
        assertEquals(2, state.getShip(0).capacity - state.getShip(0).getAvailableCapacity());
        assertEquals(tobaccoStart, state.getSupplyOf(TOBACCO));
        while (state.getCurrentPlayer() != 1) {
            // we should come back to this player for their second delivery
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertTrue(fm.computeAvailableActions(state).contains(new ShipCargo(INDIGO, 2, 2)));
        fm.next(state, new ShipCargo(INDIGO, 2, 2));
        assertEquals(startScore + 6, state.getGameScore(1), 0.01);
    }

    @Test
    public void testProceedsThroughRemainingPlayersWithGoodsToDiscard() {
        // we prompt for an action unless there are no goods to discard at all
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        // we should then skip p2
        state.getPlayerBoard(3).harvest(COFFEE, 2);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new ShipCargo(TOBACCO, 0, 2));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        // we should skip Discard Phase and move back to Role selection
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void testFullShipsSailAtEndOfCaptainRole() {
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getShip(0).load(TOBACCO, 3);
        state.getShip(1).load(INDIGO, 2);
        state.getShip(2).load(COFFEE, 2);
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        while (state.isActionInProgress()) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(5, state.getShip(0).getAvailableCapacity()); // should have been emptied
        assertEquals(2, state.getShip(1).getAvailableCapacity()); // should not have been emptied
        assertEquals(5, state.getShip(2).getAvailableCapacity()); // should not have been emptied
    }

    @Test
    public void testOptionsToKeepSingleGoodForAllPlayersWithNoWarehouses() {
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getPlayerBoard(1).harvest(TOBACCO, 1);
        state.getPlayerBoard(2).harvest(TOBACCO, 2);
        state.getPlayerBoard(2).harvest(COFFEE, 2);
        state.getPlayerBoard(2).harvest(SUGAR, 1);
        // After running post-captain processing, we expect
        // p0 to have 1 Tobacco
        // p1 to have 1 Tobacco
        // p2 to have a choice of keeping Tobacco, Sugar, or Coffee
        // p3 still to have nothing
        // we first make sure that nothing can be loaded by (illegally) putting corn on all ships
        for (Ship ship : state.getShips()) {
            ship.load(CORN, 1);
        }
        boolean discardOptionFound = false;
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        while (state.isActionInProgress()) {
            List<AbstractAction> nextActions = fm.computeAvailableActions(state);
            if (!(nextActions.get(0) instanceof DoNothing)) {
                // this should be the discard option
                if (state.getCurrentPlayer() == 0) {
                    // p0 has to discard tobacco
                    assertEquals(1, nextActions.size());
                    assertTrue(state.isActionInProgress());
                    assertTrue(nextActions.contains(new DiscardGoodsExcept(TOBACCO)));
                } else {
                    assertEquals(3, nextActions.size());
                    assertEquals(2, state.getCurrentPlayer());
                    assertTrue(state.isActionInProgress());
                    assertTrue(nextActions.contains(new DiscardGoodsExcept(TOBACCO)));
                    assertTrue(nextActions.contains(new DiscardGoodsExcept(COFFEE)));
                    assertTrue(nextActions.contains(new DiscardGoodsExcept(SUGAR)));
                    discardOptionFound = true;
                }
            }
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertTrue(discardOptionFound);
        assertEquals(1, state.getStoresOf(0, TOBACCO));
        assertEquals(1, state.getStoresOf(1, TOBACCO));
        assertEquals(1, state.getStoresOf(2, TOBACCO) + state.getStoresOf(2, COFFEE) + state.getStoresOf(2, SUGAR));
    }

    @Test
    public void smallWarehouseAutomaticallyKeepsGoods() {
        // if we have an active small warehouse, we can keep one type of good
        // and we ask for this decision first, before the single unit.
        for (Ship ship : state.getShips()) {
            ship.load(CORN, 1);
        }
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.build(0, SMALL_WAREHOUSE);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        // p0 has 2 TOBACCO only :  if we have just one type of good, then we do not need to ask any questions

        state.build(1, SMALL_WAREHOUSE);
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(1).harvest(TOBACCO, 1);
        state.getPlayerBoard(1).harvest(COFFEE, 1);
        // p0 has 1 of two good types : we do not need to ask any questions
        // but it's fine that we check which goes into warehouse

        // p2 does need to make a decision
        state.getPlayerBoard(2).harvest(TOBACCO, 1);
        state.getPlayerBoard(2).harvest(COFFEE, 1);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertTrue(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof WarehouseStorage));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // either is fine; and we should not be prompted again

        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isActionInProgress());
        assertTrue(state.getActionsInProgress().peek() instanceof DiscardPhase);

        // and check that nothing has been thrown away
        assertEquals(2, state.getStoresOf(0, TOBACCO));
        assertEquals(1, state.getStoresOf(1, TOBACCO));
        assertEquals(1, state.getStoresOf(1, COFFEE));
    }

    @Test
    public void smallWarehouseAsksForDecisionOnGoodToBeStored() {
        for (Ship ship : state.getShips()) {
            ship.load(CORN, 1);
        }
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getPlayerBoard(0).harvest(COFFEE, 3);
        state.getPlayerBoard(0).harvest(SUGAR, 1);

        state.build(0, SMALL_WAREHOUSE);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new WarehouseStorage(TOBACCO)));
        assertTrue(actions.contains(new WarehouseStorage(COFFEE)));
        assertTrue(actions.contains(new WarehouseStorage(SUGAR)));
        fm.next(state, new WarehouseStorage(TOBACCO));
        assertEquals(0, state.getCurrentPlayer());
        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new DiscardGoodsExcept(SUGAR)));
        assertTrue(actions.contains(new DiscardGoodsExcept(COFFEE)));

        fm.next(state, new DiscardGoodsExcept(COFFEE));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());

        assertEquals(2, state.getStoresOf(0, TOBACCO));
        assertEquals(1, state.getStoresOf(0, COFFEE));
        assertEquals(0, state.getStoresOf(0, SUGAR));

        assertEquals(0, state.getPlayerBoard(0).getCropsInWarehouses().size());
    }

    @Test
    public void warehousesClearedAfterCaptainAndDiscardPhase() {
        for (Ship ship : state.getShips()) {
            ship.load(CORN, 1);
        }
        state.getPlayerBoard(0).harvest(TOBACCO, 2);
        state.getPlayerBoard(0).harvest(COFFEE, 3);
        state.getPlayerBoard(0).harvest(SUGAR, 1);

        state.build(0, SMALL_WAREHOUSE);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(0, state.getPlayerBoard(0).getCropsInWarehouses().size());

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        fm.next(state, new WarehouseStorage(COFFEE));
        assertTrue(state.getPlayerBoard(0).getCropsInWarehouses().contains(COFFEE));
        assertEquals(1, state.getPlayerBoard(0).getCropsInWarehouses().size());

        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.isActionInProgress());

        assertEquals(0, state.getPlayerBoard(0).getCropsInWarehouses().size());
        assertEquals(3, state.getStoresOf(0, COFFEE));
    }

    @Test
    public void wharfProvidesAnAdditionalActionOncePublicShipActionAreExhausted() {
        // we set player 2 up with a wharf, plus CORN/COFFEE/TOBACCO/INDIGO
        // players 0 and 1 have CORN and  COFFEE respectively (to use up these ships)
        // player 3 has INDIGO only
        state.build(2, WHARF);
        state.getPlayerBoard(2).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(2).harvest(CORN, 1);
        state.getPlayerBoard(2).harvest(COFFEE, 1);
        state.getPlayerBoard(2).harvest(TOBACCO, 1);
        state.getPlayerBoard(2).harvest(INDIGO, 1);
        state.getPlayerBoard(0).harvest(CORN, 6);
        state.getPlayerBoard(1).harvest(COFFEE, 6);
        state.getPlayerBoard(3).harvest(INDIGO, 6);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getCurrentPlayer() != 2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // we *do* already yet have the Wharf option
        assertTrue(actions.stream().anyMatch(a -> a instanceof ShipCargo && ((ShipCargo) a).shipNumber == 12));

        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getCurrentPlayer() != 2);
        // at this stage all the ships are full, so we should still have the Wharf option
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(a -> a instanceof ShipCargo && ((ShipCargo) a).shipNumber == 12));
        assertTrue(actions.stream().noneMatch(a -> a instanceof ShipCargo && ((ShipCargo) a).shipNumber < 10));

        fm.next(state, actions.stream().filter(a -> a instanceof ShipCargo && ((ShipCargo) a).shipNumber == 12).findFirst().get());
        // we now cannot use the wharf for any other goods (it is currently inactive)
        assertTrue(state.getActionsInProgress().peek() instanceof DiscardPhase);

        assertTrue(state.hasActiveBuilding(2, WHARF));
    }

    @Test
    public void wharfCanTakeGoodAlreadyLoadedOnAPublicShip() {
        int totalCornShipped = 0;
        for (Ship ship : state.getShips()) {
            ship.load(CORN, ship.capacity);
            totalCornShipped += ship.capacity;
        }
        state.build(2, WHARF);
        state.getPlayerBoard(2).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(2).harvest(CORN, 3);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CAPTAIN));
        assertEquals(2, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertTrue(actions.contains(new ShipCargo( CORN, 12, 3)));
        int cornInSupply = state.getSupplyOf(CORN);
        fm.next(state, new ShipCargo( CORN, 12, 3));
        totalCornShipped += 3;
        assertFalse(state.isActionInProgress());
        assertTrue(state.hasActiveBuilding(2, WHARF));
        assertEquals(cornInSupply + totalCornShipped, state.getSupplyOf(CORN));
    }
}
