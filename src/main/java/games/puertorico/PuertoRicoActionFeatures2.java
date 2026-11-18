package games.puertorico;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.puertorico.actions.*;

import static games.puertorico.PuertoRicoConstants.Crop.CORN;

public class PuertoRicoActionFeatures2 implements IActionFeatureVector {

    private PuertoRicoActionFeatures baseFeatures = new PuertoRicoActionFeatures();

    @Override
    public String[] names() {
        return new String[] {
                "ActionType", "A_VP", "A_Cost", "A_CropValue",
                "A_ProductionCapacity", "A_InMarket", "A_InSupply", "A_InWarehouse",
                "A_Role"
        };
    }

    @Override
    public Class<?>[] types() {
        return new Class<?>[] {
                PuertoRicoActionFeatures.ActionType.class, Integer.class, Integer.class,
                Integer.class,
                Integer.class, Integer.class, Integer.class, Integer.class,
                PuertoRicoConstants.Role.class
        };
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Use featureVector instead of doubleVector for PuertoRicoActionFeatures");
    }

    @Override
    public Object[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {

        Object[] fullFeatures = baseFeatures.featureVector(action, state, playerID);

        Object[] retValue = new Object[names().length];
        for (int i = 0; i < names().length; i++) {
            for (int j = 0; j < baseFeatures.names().length; j++) {
                if (names()[i].equals(baseFeatures.names()[j])) {
                    retValue[i] = fullFeatures[j];
                    break;
                }
            }
        }
        return retValue;
    }

}
