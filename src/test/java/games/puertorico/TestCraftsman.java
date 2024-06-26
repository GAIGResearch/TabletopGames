package games.puertorico;

import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.*;
import org.junit.*;

import java.util.List;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;
import static games.puertorico.PuertoRicoConstants.Crop.*;
import static org.junit.Assert.*;

public class TestCraftsman {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void testCraftsmanSelectionUpdatesGoodsForStaffedPlantationsWithStaffedProductionBuildingsOnly() {
        // first we mark some plantations as occupied
        state.addPlantation(3, new Plantation(CORN));
        state.addPlantation(1, new Plantation(INDIGO));
        state.addPlantation(0, new Plantation(INDIGO));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.getPlayerBoard(1).getPlantations().forEach(Plantation::setOccupied);
        state.getPlayerBoard(3).getPlantations().forEach(Plantation::setOccupied);
        state.addPlantation(1, new Plantation(CORN));
        state.addPlantation(1, new Plantation(INDIGO));
        state.build(1, INDIGO_PLANT);
        state.build(0, SMALL_INDIGO_PLANT);
        state.build(3, SMALL_INDIGO_PLANT);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        state.getPlayerBoard(3).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        // At this stage p0 has Indigo (staffed) (x2) - with a small Indigo plant
        // p1 has Corn (not staffed),  Indigo (staffed) (x2), and Indigo (not staffed) - and a large Indigo Plant
        // p2 has Corn (not staffed) - no production
        // p3 has 2 Corn (staffed) - with a small Indigo plant
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(0, state.getStoresOf(0, CORN));
        assertEquals(1, state.getStoresOf(0, INDIGO));
        assertEquals(2, state.getStoresOf(1, INDIGO));
        assertEquals(0, state.getStoresOf(1, CORN));
        assertEquals(0, state.getStoresOf(2, CORN));
        assertEquals(0, state.getStoresOf(2, INDIGO));
        assertEquals(2, state.getStoresOf(3, CORN));
        assertEquals(0, state.getStoresOf(3, INDIGO));
    }

    @Test
    public void partiallyStaffedProductionBuilding() {
        // first we mark some plantations as occupied
        state.addPlantation(1, new Plantation(INDIGO));
        state.addPlantation(1, new Plantation(INDIGO));
        state.getPlayerBoard(1).getPlantations().forEach(Plantation::setOccupied);
        state.addPlantation(1, new Plantation(CORN));
        state.addPlantation(1, new Plantation(INDIGO));
        state.build(1, INDIGO_PLANT);
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(2));
        // p1 has Corn (not staffed),  Indigo (staffed) (x3), and Indigo (not staffed) - and a large Indigo Plant (with 2 staff)
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(2, state.getStoresOf(1, INDIGO));
        assertEquals(0, state.getStoresOf(1, CORN));
    }

    @Test
    public void testCraftsmanUpdatesGoodsInPlayerOrderSoShortfallIsAssigned() {
        for (int p = 0; p < 4; p++) {
            state.addPlantation(p, new Plantation(CORN));
            state.addPlantation(p, new Plantation(CORN));
            state.addPlantation(p, new Plantation(CORN));
            state.getPlayerBoard(p).getPlantations().stream().forEach(Plantation::setOccupied);
        }
        // p0 now has 1 Indigo, 3 Corn
        // p1 now has 1 Indigo, 3 Corn
        // p2 now has 4 Corn
        // p3 now has4 Corn
        assertEquals(10, state.getSupplyOf(CORN));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(3, state.getStoresOf(0, CORN));
        assertEquals(3, state.getStoresOf(1, CORN));
        assertEquals(4, state.getStoresOf(2, CORN));
        assertEquals(0, state.getStoresOf(3, CORN));
    }

    @Test
    public void testCraftsmanGivesOptionsToSelectionPlayerForBonusGood() {
        state.addPlantation(0, new Plantation(CORN));
        state.build(0, SMALL_INDIGO_PLANT);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.addPlantation(0, new Plantation(SUGAR));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        List<AbstractAction> actions = fm.computeAvailableActions(state);

        assertEquals(2, actions.size());
        assertTrue(actions.contains(new GainCrop(CORN, 1)));
        assertTrue(actions.contains(new GainCrop(INDIGO, 1)));
    }

    @Test
    public void testNoSelectionOptionIfNoGoodsProduced() {
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        // in this case we have no functioning plantations, and no option is provided
        // we should move directly to the next player
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(6, actions.size());  // the other available roles
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void testWeMoveDirectlyToNextRoleSelectionPlayer() {
        // we should have one action to pick a new good, and then move on
        state.getPlayerBoard(0).getPlantations().stream().forEach(Plantation::setOccupied);
        state.build(0, SMALL_INDIGO_PLANT);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isActionInProgress());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof SelectRole));
    }

    @Test
    public void activeFactoryGivesExtraDoubloonsWithThreeGoods() {
        state.build(0, FACTORY);
        state.addPlantation(0, new Plantation(CORN));
        state.addPlantation(0, new Plantation(INDIGO));
        state.addPlantation(0, new Plantation(SUGAR));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.build(0, SMALL_INDIGO_PLANT);
        state.build(0, SMALL_SUGAR_MILL);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(1, state.getStoresOf(0, CORN));
        assertEquals(1, state.getStoresOf(0, INDIGO));
        assertEquals(1, state.getStoresOf(0, SUGAR));
        assertEquals(4, state.getDoubloons(0));
    }

    @Test
    public void activeFactoryGivesExtraDoubloonsWithTwoGoods() {
        state.build(0, FACTORY);
        state.addPlantation(0, new Plantation(CORN));
        state.addPlantation(0, new Plantation(INDIGO));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.build(0, SMALL_INDIGO_PLANT);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(1, state.getStoresOf(0, CORN));
        assertEquals(1, state.getStoresOf(0, INDIGO));
        assertEquals(3, state.getDoubloons(0));
    }

    @Test
    public void activeFactoryGivesExtraDoubloonsWithOneGood() {
        state.build(0, FACTORY);
        state.addPlantation(0, new Plantation(CORN));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(1, state.getStoresOf(0, CORN));
        assertEquals(2, state.getDoubloons(0));
    }

    @Test
    public void activeFactoryGivesExtraDoubloonsWithFourGoods() {
        state.build(0, FACTORY);
        state.addPlantation(0, new Plantation(CORN));
        state.addPlantation(0, new Plantation(INDIGO));
        state.addPlantation(0, new Plantation(SUGAR));
        state.addPlantation(0, new Plantation(COFFEE));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.build(0, SMALL_INDIGO_PLANT);
        state.build(0, SMALL_SUGAR_MILL);
        state.build(0, COFFEE_ROASTER);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(1, state.getStoresOf(0, CORN));
        assertEquals(1, state.getStoresOf(0, INDIGO));
        assertEquals(1, state.getStoresOf(0, SUGAR));
        assertEquals(1, state.getStoresOf(0, COFFEE));
        assertEquals(5, state.getDoubloons(0));
    }

    @Test
    public void activeFactoryGivesExtraDoubloonsWithFiveGoods() {
        state.build(0, FACTORY);
        state.addPlantation(0, new Plantation(CORN));
        state.addPlantation(0, new Plantation(INDIGO));
        state.addPlantation(0, new Plantation(SUGAR));
        state.addPlantation(0, new Plantation(COFFEE));
        state.addPlantation(0, new Plantation(TOBACCO));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.build(0, SMALL_INDIGO_PLANT);
        state.build(0, SMALL_SUGAR_MILL);
        state.build(0, COFFEE_ROASTER);
        state.build(0, TOBACCO_STORAGE);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.CRAFTSMAN));
        assertEquals(1, state.getStoresOf(0, CORN));
        assertEquals(1, state.getStoresOf(0, INDIGO));
        assertEquals(1, state.getStoresOf(0, SUGAR));
        assertEquals(1, state.getStoresOf(0, COFFEE));
        assertEquals(1, state.getStoresOf(0, TOBACCO));
        assertEquals(7, state.getDoubloons(0));
    }
}
