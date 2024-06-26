package games.puertorico;

import games.puertorico.*;
import games.puertorico.components.Plantation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestVictoryPoints {


    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void buildingsAddToVictoryPointsRegardlessOfOccupation() {
        int startingVP = state.getVPSupply();
        assertEquals(0, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.HACIENDA);
        assertEquals(1, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.TOBACCO_STORAGE);
        assertEquals(4, state.getGameScore(0), 0.001);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(4, state.getGameScore(0), 0.001);
        assertEquals(startingVP, state.getVPSupply());
    }

    @Test
    public void gameEndSetWhenLastVPChipIsTaken() {
        state.addVP(2, 10);
        state.addVP(1, state.getVPSupply() - 1);
        // one left
        assertFalse(state.isLastRound());
        state.addVP(0, 1);
        assertTrue(state.isLastRound());
    }

    @Test
    public void victoryBuildingsTakeUpTwiceAsMuchSpace() {
        int startingSize = state.getPlayerBoard(0).getTownSize();
        state.build(0, PuertoRicoConstants.BuildingType.HACIENDA);
        assertEquals(startingSize + 1, state.getPlayerBoard(0).getTownSize());
        state.build(0, PuertoRicoConstants.BuildingType.RESIDENCE);
        assertEquals(startingSize + 3, state.getPlayerBoard(0).getTownSize());
    }

    @Test
    public void gameEndsSetWhenTownSpacesFilled() {
        state.build(1, PuertoRicoConstants.BuildingType.HACIENDA);
        state.build(1, PuertoRicoConstants.BuildingType.TOBACCO_STORAGE);
        state.build(1, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);
        state.build(1, PuertoRicoConstants.BuildingType.WHARF);
        state.build(1, PuertoRicoConstants.BuildingType.FACTORY);
        state.build(1, PuertoRicoConstants.BuildingType.HOSPICE);
        state.build(1, PuertoRicoConstants.BuildingType.RESIDENCE);
        state.build(1, PuertoRicoConstants.BuildingType.LARGE_MARKET);
        state.build(1, PuertoRicoConstants.BuildingType.SMALL_MARKET);
        assertEquals(10, state.getPlayerBoard(1).getTownSize());
        assertFalse(state.isLastRound());
        state.build(1, PuertoRicoConstants.BuildingType.CUSTOMS_HOUSE);
        assertEquals(12, state.getPlayerBoard(1).getTownSize());
        assertTrue(state.isLastRound());
    }

    @Test
    public void residenceVictoryPoints() {
        for (int i = 0; i < 8; i++)
            state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(0, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.RESIDENCE);
        assertEquals(4, state.getGameScore(0), 0.001);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(8, state.getGameScore(0), 0.001);
        assertEquals(9, state.getPlayerBoard(0).getPlantationSize());
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(9, state.getGameScore(0), 0.001);
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(10, state.getGameScore(0), 0.001);
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        assertEquals(11, state.getGameScore(0), 0.001);
        assertEquals(12, state.getPlayerBoard(0).getPlantationSize());
    }

    @Test
    public void guildHallVictoryPoints() {
        state.build(0, PuertoRicoConstants.BuildingType.GUILD_HALL);
        assertEquals(4, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT); // 1vp
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_SUGAR_MILL); // 1vp
        state.build(0, PuertoRicoConstants.BuildingType.TOBACCO_STORAGE); // 3vp
        assertEquals(5 + 4, state.getGameScore(0), 0.001);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(5 + 8, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.SUGAR_MILL);
        assertEquals(5 + 8 + 2 + 2, state.getGameScore(0), 0.001);
    }

    @Test
    public void fortressVictoryPoints() {
        state.build(0, PuertoRicoConstants.BuildingType.FORTRESS);  // 4vp
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);  // 1vp
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_SUGAR_MILL); // 1vp
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.INDIGO));
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.SUGAR));
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        state.getPlayerBoard(0).getPlantations().forEach(Plantation::setOccupied);
        state.addPlantation(0, new Plantation(PuertoRicoConstants.Crop.COFFEE));
        assertEquals(6, state.getPlayerBoard(0).getTotalColonists());
        assertEquals(6 + 2, state.getGameScore(0), 0.001);
        state.getPlayerBoard(0).addColonists(2);
        assertEquals(6 + 2, state.getGameScore(0), 0.001);
        state.getPlayerBoard(0).addColonists(1);
        assertEquals(6 + 3, state.getGameScore(0), 0.001);
    }

    @Test
    public void customsHouseVictoryPoints() {
        state.build(0, PuertoRicoConstants.BuildingType.CUSTOMS_HOUSE);
        state.addVP(0, 9);
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        // 1 VP for SMALL_INDIGO_PLANT, 4 VP for CUSTOMS_HOUSE, 9 VP for VP tokens = 14 VP
        assertEquals(14 + 2, state.getGameScore(0), 0.001);
        state.addVP(0, 3);
        assertEquals(17 + 3, state.getGameScore(0), 0.001);
    }

    @Test
    public void cityHallVictoryPoints() {
        state.build(0, PuertoRicoConstants.BuildingType.CITY_HALL); // 4vp
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_INDIGO_PLANT);  // 1vp, not Violet
        state.build(0, PuertoRicoConstants.BuildingType.HACIENDA);  // 1vp
        state.getPlayerBoard(0).getBuildings().forEach(b -> b.setOccupation(1));
        assertEquals(6 + 2, state.getGameScore(0), 0.001);
        state.build(0, PuertoRicoConstants.BuildingType.SMALL_MARKET); // 1vp
        assertEquals(7 + 3, state.getGameScore(0), 0.001);
    }
}


