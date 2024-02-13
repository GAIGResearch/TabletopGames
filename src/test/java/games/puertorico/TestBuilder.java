package games.puertorico;


import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.*;
import org.junit.*;

import java.util.List;
import java.util.stream.Collectors;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;
import static games.puertorico.PuertoRicoConstants.Crop.*;
import static org.junit.Assert.*;

public class TestBuilder {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void builderAllowsOptionsUpToBudget() {
        int money = state.getDoubloons(1);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, new DoNothing()); // and have player 0 do nothing with their discount
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // we start with
        assertTrue(actions.contains(new DoNothing()));
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER)));
        List<Build> builds = actions.stream()
                .filter(a -> a instanceof Build)
                .map(a -> (Build) a).collect(Collectors.toList());
        assertTrue(builds.stream().allMatch(b -> b.type.cost <= money));
        assertTrue(state.getAvailableBuildings().stream()
                .filter(b -> !builds.contains(new Build(b)))
                .allMatch(b -> b.cost > money));
    }

    @Test
    public void roleOwnerGetsDiscountOfOne() {
        int money = state.getDoubloons(0);
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // we start with
        assertTrue(actions.contains(new DoNothing()));
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER, 5)));
        List<Build> builds = actions.stream()
                .filter(a -> a instanceof Build)
                .map(a -> (Build) a).collect(Collectors.toList());
        assertTrue(builds.stream().allMatch(b -> b.type.cost <= money + 1 && b.cost <= money));
        assertTrue(state.getAvailableBuildings().stream()
                .filter(b -> builds.stream().noneMatch(b2 -> b2.type == b))
                .allMatch(b -> b.cost > money + 1));
    }

    @Test
    public void buildingExhaustionRemovesFromOptions() {
        state.changeDoubloons(0, 10);  // can afford anything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));

        state.build(1, INDIGO_PLANT);
        state.build(2, INDIGO_PLANT);
        state.build(3, INDIGO_PLANT);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // and cannot build something if none left
        assertFalse(actions.stream().anyMatch(a -> a instanceof Build && ((Build) a).type == INDIGO_PLANT));
    }

    @Test
    public void cannotBuildADuplicateBuilding() {
        state.changeDoubloons(0, 10);  // can afford anything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));

        state.build(0, INDIGO_PLANT);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // and cannot build something if we already have one
        assertFalse(actions.stream().anyMatch(a -> a instanceof Build && ((Build) a).type == INDIGO_PLANT));
    }

    @Test
    public void cannotBuildBeyondAvailableTownSlots() {
        state.build(1, PuertoRicoConstants.BuildingType.HACIENDA);
        state.build(1, PuertoRicoConstants.BuildingType.TOBACCO_STORAGE);
        state.build(1, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);
        state.build(1, PuertoRicoConstants.BuildingType.WHARF);
        state.build(1, PuertoRicoConstants.BuildingType.FACTORY);
        state.build(1, PuertoRicoConstants.BuildingType.HOSPICE);
        state.build(1, PuertoRicoConstants.BuildingType.RESIDENCE);
        state.build(1, PuertoRicoConstants.BuildingType.LARGE_MARKET);
        state.build(1, PuertoRicoConstants.BuildingType.SMALL_MARKET);
        state.build(1, SMALL_WAREHOUSE);
        assertEquals(11, state.getPlayerBoard(1).getTownSize());

        state.changeDoubloons(1, 10); // so they can afford anything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // to move to player 1
        List<AbstractAction> actions = fm.computeAvailableActions(state);

        assertTrue(actions.contains(new Build(PuertoRicoConstants.BuildingType.INDIGO_PLANT)));
        assertFalse(actions.contains(new Build(FORTRESS)));
        assertTrue(actions.stream().filter(a -> a instanceof Build).map(a -> (Build) a).allMatch(b -> b.type.size == 1));
    }

    @Test
    public void cannotBuildMoreThanFourLargeBuildings() {
        state.build(1, RESIDENCE);
        state.build(1, CUSTOMS_HOUSE);
        state.build(1, FORTRESS);
        state.build(1, GUILD_HALL);

        assertEquals(8, state.getPlayerBoard(1).getTownSize());

        state.changeDoubloons(1, 10); // so they can afford anything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // to move to player 1
        List<AbstractAction> actions = fm.computeAvailableActions(state);

        assertTrue(actions.contains(new Build(PuertoRicoConstants.BuildingType.INDIGO_PLANT)));
        assertFalse(actions.contains(new Build(CITY_HALL)));
        assertTrue(actions.stream().filter(a -> a instanceof Build).map(a -> (Build) a).allMatch(b -> b.type.size == 1));
    }

    @Test
    public void eachPlayerHasATurnToBuild() {
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void quarryProvidesDiscount() {
        Plantation quarry = new Plantation(QUARRY);
        quarry.setOccupied();
        state.addPlantation(0, quarry);
        int money = state.getDoubloons(0);
        // with one quarry we get a discount of 1 on everything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // note that this includes the discount of 1 as role owner
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertTrue(actions.contains(new Build(SMALL_SUGAR_MILL, 0)));
        assertTrue(actions.contains(new Build(SUGAR_MILL, 2)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER, 4)));

        // with two quarries we get a discount of 2 on a SUGAR_MILL, but only 1 on a SMALL_SUGAR_MILL
        quarry = new Plantation(QUARRY);
        quarry.setOccupied();
        state.addPlantation(0, quarry);
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertTrue(actions.contains(new Build(SMALL_SUGAR_MILL, 0)));
        assertTrue(actions.contains(new Build(SUGAR_MILL, 1)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER, 3)));

        // with three quarries we get a discount on the COFFEE_ROASTER
        quarry = new Plantation(QUARRY);
        quarry.setOccupied();
        state.addPlantation(0, quarry);
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertTrue(actions.contains(new Build(SMALL_SUGAR_MILL, 0)));
        assertTrue(actions.contains(new Build(SUGAR_MILL, 1)));
        assertTrue(actions.contains(new Build(COFFEE_ROASTER, 2)));
    }

    @Test
    public void quarriesOnlyProvideDiscountIfStaffed() {
        Plantation quarry = new Plantation(QUARRY);
        quarry.setOccupied();
        state.addPlantation(0, quarry);
        int money = state.getDoubloons(0);
        // with one quarry we get a discount of 1 on everything
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // note that this includes the discount of 1 as role owner
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertTrue(actions.contains(new Build(SMALL_SUGAR_MILL, 0)));
        assertTrue(actions.contains(new Build(SUGAR_MILL, 2)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER, 4)));

        // with two quarries we get a discount of 2 on a SUGAR_MILL, but only 1 on a SMALL_SUGAR_MILL
        quarry = new Plantation(QUARRY);
        state.addPlantation(0, quarry);
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new Build(SMALL_INDIGO_PLANT, 0)));
        assertTrue(actions.contains(new Build(SMALL_SUGAR_MILL, 0)));
        assertTrue(actions.contains(new Build(SUGAR_MILL, 2)));
        assertFalse(actions.contains(new Build(COFFEE_ROASTER, 4)));
    }

    @Test
    public void inactiveUniversityDoesNotAddColonistToBuilding() {
        state.build(1, UNIVERSITY);
        int startingColonists = state.getColonistsInSupply();
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move to p1

        assertEquals(1, state.getCurrentPlayer());
        Build build = (Build) fm.computeAvailableActions(state).get(0);
        fm.next(state, build); // build
        assertFalse(state.hasActiveBuilding(1, build.type));
        assertEquals(startingColonists, state.getColonistsInSupply());
    }

    @Test
    public void activeUniversityDoesAddColonistToBuilding() {
        state.build(1, UNIVERSITY);
        int startingColonists = state.getColonistsInSupply();
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(1));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.BUILDER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move to p1

        assertEquals(1, state.getCurrentPlayer());
        Build build = (Build) fm.computeAvailableActions(state).get(0);
        fm.next(state, build); // build
        assertTrue(state.hasActiveBuilding(1, build.type));
        assertEquals(startingColonists - 1, state.getColonistsInSupply());
    }

}
