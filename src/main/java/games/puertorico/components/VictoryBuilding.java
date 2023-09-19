package games.puertorico.components;

import games.puertorico.*;

import java.util.List;


public class VictoryBuilding extends Building {

    public VictoryBuilding(PuertoRicoConstants.BuildingType type) {
        super(type);
    }

    public int getVictoryPoints(PuertoRicoGameState state, int player) {
        PuertoRicoParameters params = (PuertoRicoParameters) state.getGameParameters();
        switch (buildingType) {
            case GUILD_HALL:
                int total = 0;
                for (Building b : state.getPlayerBoard(player).getBuildings()) {
                    switch (b.buildingType) {
                        case SMALL_INDIGO_PLANT:
                        case SMALL_SUGAR_MILL:
                            total++;
                            break;
                        case INDIGO_PLANT:
                        case SUGAR_MILL:
                        case TOBACCO_STORAGE:
                        case COFFEE_ROASTER:
                            total += 2;
                            break;
                        default:
                    }
                }
                return total;
            case CUSTOMS_HOUSE:
                return state.getPlayerBoard(player).getVP() / params.customsHouseDenominator;
            case RESIDENCE:
                int totalPlantations = state.getPlayerBoard(player).getPlantationSize();
                return 4 + Math.max(totalPlantations - 9, 0);
            case CITY_HALL:
                List<Building> buildings = state.getPlayerBoard(player).getBuildings();
                int count = 0;
                for (Building b : buildings) {
                    switch(b.buildingType) {
                        case SMALL_INDIGO_PLANT:
                        case SMALL_SUGAR_MILL:
                        case INDIGO_PLANT:
                        case SUGAR_MILL:
                        case TOBACCO_STORAGE:
                        case COFFEE_ROASTER:
                            break;
                        default:
                            count++;
                    }
                }
                return count;
            case FORTRESS:
                return state.getPlayerBoard(player).getTotalColonists() / params.fortressDenominator;
            default:
                throw new AssertionError("Unexpected victory building type " + buildingType);
        }
    }
}
