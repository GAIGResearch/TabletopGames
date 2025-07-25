package games.puertorico;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.puertorico.actions.Build;
import games.puertorico.actions.BuildQuarry;
import games.puertorico.actions.DiscardGoodsExcept;
import games.puertorico.actions.DrawPlantation;

public class PuertoRicoActionFeatures implements IActionFeatureVector {

    @Override
    public String[] names() {
        return new String[] {
                "ActionType", "Building", "BuildVP", "BuildCost", "CropType", "CropValue",
                "ProductionCapacity", "InMarket", "InSupply", "InWarehouse",
                "SelectRoleType"
        };
    }

    @Override
    public Class<?>[] types() {
        return new Class<?>[] {
                String.class, PuertoRicoConstants.BuildingType.class, Integer.class, Integer.class,
                PuertoRicoConstants.Crop.class, Integer.class,
                Integer.class, Boolean.class, Integer.class, Integer.class,
                PuertoRicoConstants.Role.class
        };
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Use featureVector instead of doubleVector for PuertoRicoActionFeatures");
    }

    @Override
    public Object[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        PuertoRicoGameState prs = (PuertoRicoGameState) state;

        Object[] retValue = new Object[names().length];
        retValue[0] = action.getClass().getSimpleName();
        if (action instanceof Build build) {
            retValue[1] = build.type;
            retValue[2] = build.type.vp;
            retValue[3] = build.cost;
            retValue[4] = null; /// TODO: If production building, then this should be crop
            retValue[5] = null; // TODO: If production building, then this should be crop
            retValue[6] = null; // TODO: If production building, then this should be the amount that will be immediately usabl
        } else if (action instanceof BuildQuarry bq){
            retValue[4] = PuertoRicoConstants.Crop.QUARRY; // Quarry does not produce crops
            retValue[5] = 0; // Quarry does not produce crops
            retValue[6] = 1;
        } else if (action instanceof DiscardGoodsExcept discard){
            setCropFeatures(prs, playerID, discard.crop, retValue);
        } else if (action instanceof DrawPlantation){
        }
        return retValue;
    }

    private void setCropFeatures(PuertoRicoGameState prs, int playerID, PuertoRicoConstants.Crop crop, Object[] retValue) {
        retValue[4] = crop;
        retValue[5] = crop.price;
        retValue[7] = prs.soldInMarket.contains(crop);
        retValue[8] = prs.getSupplyOf(crop);
        retValue[9] = prs.getPlayerBoard(playerID).getStoresOf(crop);
    }

}
