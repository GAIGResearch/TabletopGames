package games.puertorico;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.Plantation;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.puertorico.PuertoRicoConstants.Role.SETTLER;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

public class TestSettlerAndPlantations {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void settlerPrivilegeAllowsQuarryConstruction() {
        fm.next(state, new SelectRole(SETTLER));
        assertEquals(0, state.getCurrentPlayer());
        // then check that the available actions match up with the visible plantations
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<PuertoRicoConstants.Crop> uniqueCrops = state.getAvailablePlantations().getComponents().stream().map(p -> p.crop).collect(toSet());
        assertEquals(uniqueCrops.size() + 2, actions.size());
        assertTrue(actions.contains(new BuildQuarry()));
        for (PuertoRicoConstants.Crop p : uniqueCrops) {
            assertTrue(actions.contains(new DrawPlantation(p)));
        }
    }

    @Test
    public void buildQuarryNotPossibleIfNoneLeft() {
        while (state.getQuarriesLeft() > 0)
            state.removeQuarry();
        fm.next(state, new SelectRole(SETTLER));
        assertEquals(0, state.getCurrentPlayer());
        // then check that the available actions match up with the visible plantations
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<PuertoRicoConstants.Crop> uniqueCrops = state.getAvailablePlantations().getComponents().stream().map(p -> p.crop).collect(toSet());
        assertEquals(uniqueCrops.size() + 1, actions.size());
        assertFalse(actions.contains(new BuildQuarry()));
    }

    @Test
    public void plantationsRefreshedAfterSettlerAction() {
        fm.next(state, new SelectRole(SETTLER));
        int start = state.numberOfPlantationsInStack();
        fm.next(state, fm.computeAvailableActions(state).get(0)); // make an action for the settler (checked in previous unit test)
        for (int i = 1; i < 4; i++) {
            assertEquals(i, state.getCurrentPlayer());
            // then check that the available actions match up with the visible plantations
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            Set<PuertoRicoConstants.Crop> uniqueCrops = state.getAvailablePlantations().getComponents().stream().map(p -> p.crop).collect(toSet());
            assertEquals(uniqueCrops.size() + 1, actions.size());
            for (PuertoRicoConstants.Crop p : uniqueCrops) {
                assertTrue(actions.contains(new DrawPlantation(p)));
            }
            assertEquals(5 - i, state.getAvailablePlantations().getSize());  // we have replenished
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(5, state.getAvailablePlantations().getSize());  // we have replenished
        assertEquals(start - 5, state.numberOfPlantationsInStack());
        assertEquals(1, state.numberOfPlantationsInDiscard());
        assertEquals(2, state.getPlayerBoard(0).getPlantations().size());
        assertEquals(2, state.getPlayerBoard(1).getPlantations().size());
        assertEquals(2, state.getPlayerBoard(2).getPlantations().size());
        assertEquals(2, state.getPlayerBoard(3).getPlantations().size());
    }

    @Test
    public void eachPlayerTakesTurnInCurrentRole() {
        assertEquals(0, state.getTurnCounter());
        fm.next(state, new SelectRole(SETTLER));
        for (int i = 0; i < 4; i++) {
            System.out.println("Player " + i + " is taking a turn as " + fm.computeAvailableActions(state).get(0));
            assertEquals(i, state.getCurrentPlayer());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void occupiedConstructionHutAllowsQuarryToBeTakenIfThereIsOneLeft() {
        state.build(1, PuertoRicoConstants.BuildingType.CONSTRUCTION_HUT);
        fm.next(state, new SelectRole(SETTLER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past the roleOwner
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new BuildQuarry()));
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(1));
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new BuildQuarry()));
        while (state.getQuarriesLeft() > 0)
            state.removeQuarry();
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new BuildQuarry()));
    }

    @Test
    public void buildQuarryDoes() {
        assertEquals(0, state.getPlayerBoard(0).getPlantationsOf(PuertoRicoConstants.Crop.QUARRY));
        fm.next(state, new BuildQuarry());
        assertEquals(7, state.getQuarriesLeft());
        assertEquals(1, state.getPlayerBoard(0).getPlantationsOf(PuertoRicoConstants.Crop.QUARRY));
        assertEquals(2, state.getDoubloons(0));
    }

    @Test
    public void occupiedHaciendaGivesAdditionalStepOfDrawingPlantationBeforeMainDecision() {
        state.build(1, PuertoRicoConstants.BuildingType.HACIENDA);
        state.getPlayerBoard(1).getBuildings().forEach(b -> b.setOccupation(1));
        fm.next(state, new SelectRole(SETTLER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past the roleOwner
        assertEquals(1, state.getCurrentPlayer());

        int startingStackSize = state.numberOfPlantationsInStack();

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new DrawPlantation(null), actions.get(0));
        assertEquals(new DoNothing(), actions.get(1));

        fm.next(state, actions.get(0));
        assertEquals(startingStackSize - 1, state.numberOfPlantationsInStack());
        assertEquals(2, state.getPlayerBoard(1).getPlantations().size());
        assertEquals(1, state.getCurrentPlayer());
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new DrawPlantation(null)));
        assertTrue(actions.stream().anyMatch(a -> a instanceof DrawPlantation));
        fm.next(state, actions.get(0));
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void unoccupiedHospiceWillNotGrantAdditionalColonistForPlantation() {
        int startingColonists = state.getColonistsInSupply();
        state.build(2, PuertoRicoConstants.BuildingType.HOSPICE);
        fm.next(state, new SelectRole(SETTLER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past the roleOwner
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past p1
        assertEquals(2, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        fm.next(state, actions.get(0));
        // this should not give a free colonist
        assertTrue(state.getPlayerBoard(2).getPlantations().stream().noneMatch(Plantation::isOccupied));
        assertEquals(2, state.getPlayerBoard(2).getPlantationVacancies());
        assertEquals(startingColonists, state.getColonistsInSupply());
    }
    @Test
    public void occupiedHospiceWillGrantAdditionalColonistForPlantation() {
        int startingColonists = state.getColonistsInSupply();
        state.build(2, PuertoRicoConstants.BuildingType.HOSPICE);
        state.getPlayerBoard(2).getBuildings().forEach(b -> b.setOccupation(1));
        fm.next(state, new SelectRole(SETTLER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past the roleOwner
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past p1
        assertEquals(2, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        fm.next(state, actions.get(0));
        PuertoRicoConstants.Crop newPlantation = ((DrawPlantation)actions.get(0)).crop;
        // this should give a free colonist
        assertEquals(1, state.getPlayerBoard(2).getPlantations().stream().filter(Plantation::isOccupied).filter(p -> p.crop == newPlantation).count());
        assertEquals(1, state.getPlayerBoard(2).getPlantationVacancies());
        assertEquals(startingColonists - 1, state.getColonistsInSupply());
    }

    @Test
    public void hospiceDoesNotGiveFreeColonistToHaciendaPlantation() {
        int startingColonists = state.getColonistsInSupply();
        state.build(2, PuertoRicoConstants.BuildingType.HOSPICE);
        state.build(2, PuertoRicoConstants.BuildingType.HACIENDA);
        state.getPlayerBoard(2).getBuildings().forEach(b -> b.setOccupation(1));
        fm.next(state, new SelectRole(SETTLER));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past the roleOwner
        fm.next(state, fm.computeAvailableActions(state).get(0)); // move past p1
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // should be DrawPlantation from stack
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        PuertoRicoConstants.Crop newPlantation = ((DrawPlantation)actions.get(0)).crop;
        fm.next(state, actions.get(0));
        // this should give a free colonist
        assertEquals(1, state.getPlayerBoard(2).getPlantations().stream().filter(Plantation::isOccupied).filter(p -> p.crop == newPlantation).count());
        assertEquals(2, state.getPlayerBoard(2).getPlantationVacancies()); // due to Hacienda
        assertEquals(startingColonists - 1, state.getColonistsInSupply());
    }


}
