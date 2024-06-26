package games.puertorico;

import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.puertorico.PuertoRicoConstants.Role.MAYOR;
import static org.junit.Assert.*;

public class TestMayor {
    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void testMayorGetsBonusColonist() {
        // we add 2 plantations to each player so that no-one automatically moves on
        for (int i = 0; i < 4; i++) {
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.INDIGO));
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.CORN));
        }
        fm.next(state, new SelectRole(MAYOR));
        assertEquals(0, state.getColonistsOnShip());
        assertEquals(2, state.getPlayerBoard(0).getUnassignedColonists());
        for (int i = 1; i < 4; i++) {
            assertEquals(1, state.getPlayerBoard(i).getUnassignedColonists());
        }
    }

    @Test
    public void testColonistsDistributedUnevenly() {
        // we add 2 plantations to each player so that no-one automatically moves on
        for (int i = 0; i < 4; i++) {
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.INDIGO));
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.CORN));
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        }
        state.changeColonistsOnShip(2); // p0 and p1 now get an extra colonist
        fm.next(state, new SelectRole(MAYOR));
        assertEquals(3, state.getPlayerBoard(0).getUnassignedColonists());
        assertEquals(2, state.getPlayerBoard(1).getUnassignedColonists());
        assertEquals(1, state.getPlayerBoard(2).getUnassignedColonists());
        assertEquals(1, state.getPlayerBoard(3).getUnassignedColonists());
    }

    @Test
    public void testPlayerSkippedIfTheyCanFillAllVacancies() {
        for (int i = 0; i < 4; i++) {
            state.addPlantation(i, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        }
        state.changeColonistsOnShip(2); // 6 on ship
        fm.next(state, new SelectRole(MAYOR));
        assertEquals(1, state.getPlayerBoard(0).getUnassignedColonists());
        assertEquals(0, state.getPlayerBoard(1).getUnassignedColonists());
        assertEquals(1, state.getPlayerBoard(2).getUnassignedColonists());
        assertEquals(1, state.getPlayerBoard(3).getUnassignedColonists());
        // In this case, p0 and p1 have 2 Indigo, while p2 and p3 have 1 Indigo + 1 corn (so must decide)
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());
        assertTrue(state.isActionInProgress());
    }

    @Test
    public void testPlayerIsGivenOptionsToFillVacanciesUntilAllRemainingCanBeFilled() {
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO)); // p0 has 3 Indigo
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.TOBACCO)); // p1 has Indigo, Tobacco...and is therefore the only player to make a decision
        // p2 and p3 have one plantation, which is auto-filled

        fm.next(state, new SelectRole(MAYOR));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.TOBACCO)));
        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void testEndOfMayorRepopulatesColonists() {
        int colonistSupply = state.getColonistsInSupply();
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO)); // p0 has 3 Indigo
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.TOBACCO)); // p1 has Indigo, Tobacco
        // p2 and p3 have one plantation, which is auto-filled

        fm.next(state, new SelectRole(MAYOR));
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.isActionInProgress());

        assertEquals(4, state.getColonistsOnShip());
        assertEquals(colonistSupply - 5, state.getColonistsInSupply()); // 1 to Mayor, and 4 to ship
    }

    @Test
    public void testPlantationColonistsGoToDifferentPlantations() {
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.TOBACCO));
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_MARKET);
        // p0 now has 2 indigo and 1 tobacco
        assertEquals(3, state.getPlayerBoard(0).getPlantationSize());
        assertEquals(3, state.getPlayerBoard(0).getPlantationVacancies());
        fm.next(state, new SelectRole(MAYOR));
        fm.next(state, new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(3, state.getPlayerBoard(0).getPlantationSize());
        assertEquals(2, state.getPlayerBoard(0).getPlantationVacancies());
        fm.next(state, new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(1, state.getPlayerBoard(0).getPlantationVacancies());
    }

    @Test
    public void testWeStopAskingOnceAllColonistsUsed() {
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.TOBACCO));
        state.getPlayerBoard(1).addColonists(1);
        // p1 has 4 plantations, and 2 settlers, so they should get two decisions (if the first one is INDIGO)

        fm.next(state, new SelectRole(MAYOR));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.TOBACCO)));

        fm.next(state, new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO));
        actions = fm.computeAvailableActions(state);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.TOBACCO)));

        fm.next(state, actions.get(0));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getPlayerBoard(1).getUnassignedColonists());
    }

    @Test
    public void testWeStopAskingOnceRemainingColonistsSuffice() {
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.TOBACCO));
        state.getPlayerBoard(1).addColonists(1);
        // p1 has 4 plantations, and 2 settlers, so they should get one decision (if the first one is TOBACCO)

        fm.next(state, new SelectRole(MAYOR));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.TOBACCO)));

        fm.next(state, new OccupyPlantation(PuertoRicoConstants.Crop.TOBACCO));

        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getPlayerBoard(1).getUnassignedColonists());
    }

    @Test
    public void testColonistsExhaustedTriggersGameEnd() {
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.TOBACCO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.COFFEE));
        state.changeColonistsInSupply(-state.getColonistsInSupply() + 2);
        fm.next(state, new SelectRole(MAYOR));
        assertFalse(state.isLastRound());
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.isActionInProgress());
        assertTrue(state.isLastRound());
    }

    @Test
    public void buildingsCanBeOccupiedAsWellAsPlantations() {
        state.addPlantation(1, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.build(1, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);
        state.build(1, PuertoRicoConstants.BuildingType.SUGAR_MILL);
        state.getPlayerBoard(1).addColonists(2);

        fm.next(state, new SelectRole(MAYOR)); // now has three to place, and 6 vacancies across buildings and plantations
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(3, state.getPlayerBoard(1).getUnassignedColonists());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyBuilding(PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT)));
        assertTrue(actions.contains(new OccupyBuilding(PuertoRicoConstants.BuildingType.SUGAR_MILL)));

        ProductionBuilding smallIndigo = (ProductionBuilding) state.getPlayerBoard(1).getBuildings().stream()
                .filter(b -> b.buildingType == PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT).findFirst().orElseThrow(
                        () -> new IllegalStateException("No small indigo plant found"));
        ProductionBuilding sugarMill = (ProductionBuilding) state.getPlayerBoard(1).getBuildings().stream()
                .filter(b -> b.buildingType == PuertoRicoConstants.BuildingType.SUGAR_MILL).findFirst().orElseThrow(
                        () -> new IllegalStateException("No sugar mill found"));

        fm.next(state, new OccupyBuilding(PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT));
        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyBuilding(PuertoRicoConstants.BuildingType.SUGAR_MILL)));

        assertEquals(1, smallIndigo.getOccupation());
        assertEquals(0, sugarMill.getOccupation());

        fm.next(state, new OccupyBuilding(PuertoRicoConstants.BuildingType.SUGAR_MILL));
        assertEquals(1, smallIndigo.getOccupation());
        assertEquals(1, sugarMill.getOccupation());
        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new OccupyPlantation(PuertoRicoConstants.Crop.INDIGO)));
        assertTrue(actions.contains(new OccupyBuilding(PuertoRicoConstants.BuildingType.SUGAR_MILL)));

    }
}
