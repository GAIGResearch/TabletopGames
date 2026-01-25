package games.puertorico;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.puertorico.PuertoRicoConstants.Crop;
import games.puertorico.components.*;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class PuertoRicoFeaturesBase implements IStateFeatureVector {

    @Override
    public String[] names() {
        return new String[] {"VP_Chips", "VP", "TownSize", "PlantationSize", "Quarries", "TownVacancies",
                "PlantationVacancies", "Doubloons", "ProductionCapacity", "ProductionVariety", "StoreVolume", "StoreVariety"};
    }
    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        double[] retValue = new double[names().length];

        retValue[0] = state.getPlayerBoard(playerID).vp;
        retValue[1] = state.getGameScore(playerID);
        retValue[2] = state.getPlayerBoard(playerID).getTownSize();
        retValue[3] = state.getPlayerBoard(playerID).getPlantationSize();
        retValue[4] = state.getPlayerBoard(playerID).getPlantations().stream().filter(p -> p.crop == Crop.QUARRY).count();
        retValue[5] = state.getPlayerBoard(playerID).getTownVacancies();
        retValue[6] = state.getPlayerBoard(playerID).getPlantationVacancies();
        retValue[7] = state.getPlayerBoard(playerID).getDoubloons();
        // The next two tie up plantations and production facilities for the same goods:
        int goodTypes = 0;
        int goodVolume = 0;
        Map<Crop, Long> plantationsByCrop = state.getPlayerBoard(playerID).getPlantations().stream()
                .filter(Plantation::isOccupied)
                .collect(groupingBy(p -> p.crop, Collectors.counting()));
        Map<Crop, Integer> productionByCrop = state.getPlayerBoard(playerID).getBuildings().stream()
                .filter(b -> b instanceof ProductionBuilding)
                .collect(groupingBy(b -> ((ProductionBuilding) b).cropType, Collectors.collectingAndThen(Collectors.summingInt(Building::getOccupation), Integer::intValue)));
        for (Crop c : plantationsByCrop.keySet()) {
            if (c == Crop.CORN && plantationsByCrop.get(c) > 0) {
                goodTypes++;
                goodVolume += plantationsByCrop.get(c);
            } else if (plantationsByCrop.get(c) > 0 && productionByCrop.getOrDefault(c, 0) > 0) {
                goodTypes++;
                goodVolume += (int) Math.min(plantationsByCrop.get(c), productionByCrop.get(c));
            }
        }
        retValue[8] = goodVolume;
        retValue[9] = goodTypes;
        retValue[10] = state.getPlayerBoard(playerID).getStores().values().stream().mapToInt(Integer::intValue).sum();
        retValue[11] = state.getPlayerBoard(playerID).getStores().values().stream().filter(i -> i > 0).count();

        return retValue;
    }


}
