package games.puertorico;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

import java.util.Arrays;

public class PuertoRicoFeaturesBuildings implements IStateFeatureVector {

    PuertoRicoFeaturesBase baseFeatures = new PuertoRicoFeaturesBase();

    @Override
    public String[] names() {
        String base[] = baseFeatures.names();
        String[] buildings = Arrays.stream(PuertoRicoConstants.BuildingType.values()).map(PuertoRicoConstants.BuildingType::toString).toArray(String[]::new);
        String[] retValue = new String[base.length + buildings.length];
        System.arraycopy(base, 0, retValue, 0, base.length);
        System.arraycopy(buildings, 0, retValue, base.length, buildings.length);
        return retValue;
    }
    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        double[] retValue = new double[names().length];

        double[] base = baseFeatures.doubleVector(gs, playerID);
        System.arraycopy(base, 0, retValue, 0, base.length);

        for (int i = 0; i < PuertoRicoConstants.BuildingType.values().length; i++) {
            int finalI = i;
            retValue[i + base.length] = state.getPlayerBoard(playerID).getBuildings().stream()
                    .filter(b -> b.buildingType == PuertoRicoConstants.BuildingType.values()[finalI])
                    .count();
        }
        return retValue;
    }


}
