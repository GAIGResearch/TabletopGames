package games.puertorico;

import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.SelectRole;
import games.puertorico.actions.Sell;
import org.junit.Before;
import org.junit.Test;

import static games.puertorico.PuertoRicoConstants.Crop.*;
import static org.junit.Assert.*;

public class TestTrader {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void playersReceiveAppropriateMoniesForSale() {
        state.getPlayerBoard(0).harvest(COFFEE, 1);
        state.getPlayerBoard(1).harvest(INDIGO, 1);
        state.getPlayerBoard(2).harvest(CORN, 1);
        state.getPlayerBoard(3).harvest(SUGAR, 1);
        int startingIndigo = state.getSupplyOf(INDIGO);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(0, state.getCurrentPlayer());
        for (PuertoRicoConstants.Crop c : PuertoRicoConstants.Crop.values()) {
            assertFalse(state.getMarket().contains(c));
        }
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE
        assertEquals(7, state.getDoubloons(0));  // +1 bonus from roleOwner
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.getMarket().contains(COFFEE));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // INDIGO
        assertEquals(3, state.getDoubloons(1));
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.getMarket().contains(INDIGO));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // CORN
        assertEquals(2, state.getDoubloons(2));
        assertEquals(3, state.getCurrentPlayer());
        assertTrue(state.getMarket().contains(CORN));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // SUGAR
        assertEquals(4, state.getDoubloons(3));
        assertEquals(1, state.getCurrentPlayer());
        // this should end the role, and hence clear the (full) market
        assertFalse(state.getMarket().contains(SUGAR));
        assertTrue(state.getMarket().isEmpty());
        assertEquals(startingIndigo + 1, state.getSupplyOf(INDIGO));
        assertEquals(0, state.getStoresOf(0, COFFEE));
        assertEquals(0, state.getStoresOf(1, INDIGO));
        assertEquals(0, state.getStoresOf(2, CORN));
        assertEquals(0, state.getStoresOf(3, SUGAR));
    }

    @Test
    public void playersWithNothingToTradeAreAutoSkipped() {
        state.getPlayerBoard(0).harvest(COFFEE, 1);
        state.getPlayerBoard(3).harvest(SUGAR, 1);
        int startingCoffee = state.getSupplyOf(COFFEE);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // SUGAR
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
        assertEquals(2, state.getMarket().size());
        assertTrue(state.getMarket().contains(COFFEE));
        assertTrue(state.getMarket().contains(SUGAR));
        assertEquals(startingCoffee, state.getSupplyOf(COFFEE));
    }

    @Test
    public void playersCannotSellGoodsAlreadyInTheMarket() {
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        state.getPlayerBoard(2).harvest(COFFEE, 1);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE
        fm.next(state, fm.computeAvailableActions(state).get(0)); // INDIGO
        // then we skip p2 because they have nothing to sell now
        assertEquals(6, fm.computeAvailableActions(state).size());
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());

        assertEquals(2, state.getMarket().size());
    }

    @Test
    public void playersCannotSellGoodsAlreadyInTheMarketUnlessTheyHaveAnOffice() {
        state.build(2, PuertoRicoConstants.BuildingType.OFFICE);
        state.getPlayerBoard(2).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        state.getPlayerBoard(2).harvest(COFFEE, 1);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE, p0
        fm.next(state, fm.computeAvailableActions(state).get(0)); // INDIGO, p1
        // we now do not skip p2, because they can sell their COFFEE
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isActionInProgress());
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertEquals(new Sell(COFFEE, 4), fm.computeAvailableActions(state).get(0));
        assertEquals(new Sell(null, 0), fm.computeAvailableActions(state).get(1));
        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(3, state.getMarket().size());
        assertEquals(6, state.getDoubloons(2));
    }

    @Test
    public void onePlayerCannotSellTwoDifferentGoods() {
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        state.getPlayerBoard(1).harvest(TOBACCO, 2);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE
        fm.next(state, fm.computeAvailableActions(state).get(0)); // INDIGO or TOBACCO

        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());  // we have moved on to SelectRole
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof SelectRole));
    }

    @Test
    public void cannotSellAnythingIfMarketIsFull() {
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        state.getPlayerBoard(1).harvest(INDIGO, 2);
        state.getPlayerBoard(2).harvest(CORN, 2);
        state.getPlayerBoard(3).harvest(SUGAR, 2);
        state.getPlayerBoard(3).harvest(TOBACCO, 2);

        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // COFFEE
        fm.next(state, fm.computeAvailableActions(state).get(0)); // INDIGO
        fm.next(state, fm.computeAvailableActions(state).get(0)); // CORN
        fm.next(state, fm.computeAvailableActions(state).get(0)); // SUGAR or TOBACCO
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getMarket().size());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void onceAPlayerHasPassedTheyDoNotGetAskedAgain() {
        // this is to avoid infinite loops
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        state.getPlayerBoard(0).harvest(INDIGO, 2);
        state.getPlayerBoard(1).harvest(CORN, 2);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(3, fm.computeAvailableActions(state).size());
        fm.next(state, fm.computeAvailableActions(state).get(2)); // Nothing
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // CORN
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void occupiedMarketIncreasesSalesValue() {
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_MARKET);
        state.getPlayerBoard(0).harvest(COFFEE, 2);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.TRADER));
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertEquals(new Sell(COFFEE, 5), fm.computeAvailableActions(state).get(0));
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(new Sell(COFFEE, 6), fm.computeAvailableActions(state).get(0));

        state.build(0, PuertoRicoConstants.BuildingType.LARGE_MARKET);
        assertEquals(new Sell(COFFEE, 6), fm.computeAvailableActions(state).get(0));
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(new Sell(COFFEE, 8), fm.computeAvailableActions(state).get(0));
    }


}
