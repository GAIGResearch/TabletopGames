package games.puertorico;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

import java.util.Arrays;

public class PuertoRicoFeaturesBuildings implements IStateFeatureVector {

    String[] base = new String[] {"VP", "Doubloons", "Stores", "PlantationSize", "Vacancies"};
    @Override
    public String[] names() {
        String[] buildings = Arrays.stream(PuertoRicoConstants.BuildingType.values()).map(PuertoRicoConstants.BuildingType::toString).toArray(String[]::new);
        String[] retValue = new String[base.length + buildings.length];
        System.arraycopy(base, 0, retValue, 0, base.length);
        System.arraycopy(buildings, 0, retValue, base.length, buildings.length);
        return retValue;
    }
    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        double[] retValue = new double[names().length];

        retValue[0] = state.getGameScore(playerID);
        retValue[1] = state.getPlayerBoard(playerID).getDoubloons();
        retValue[2] = state.getPlayerBoard(playerID).getStores().values().stream().mapToInt(Integer::intValue).sum();
        retValue[3] = state.getPlayerBoard(playerID).getPlantationSize();
        retValue[4] = state.getPlayerBoard(playerID).getPlantationVacancies() + state.getPlayerBoard(playerID).getTownVacancies();

        for (int i = 0; i < PuertoRicoConstants.BuildingType.values().length; i++) {
            int finalI = i;
            retValue[i + base.length] = state.getPlayerBoard(playerID).getBuildings().stream()
                    .filter(b -> b.buildingType == PuertoRicoConstants.BuildingType.values()[finalI])
                    .count();
        }
        return retValue;
    }


}
