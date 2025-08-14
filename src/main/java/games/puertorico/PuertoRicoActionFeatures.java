package games.puertorico;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.puertorico.actions.*;

import static games.puertorico.PuertoRicoConstants.Crop.CORN;

public class PuertoRicoActionFeatures implements IActionFeatureVector {

    public enum ActionType {
        BUILD, BUILD_QUARRY, DISCARD_GOODS_EXCEPT, DRAW_PLANTATION, GAIN_CROP,
        OCCUPY_BUILDING, OCCUPY_PLANTATION, SELECT_ROLE, SELL, SHIP_CARGO, WAREHOUSE_STORAGE
    }

    @Override
    public String[] names() {
        return new String[] {
                "ActionType", "A_Building", "A_VP", "A_Cost", "A_CropType", "A_CropValue",
                "A_ProductionCapacity", "A_InMarket", "A_InSupply", "A_InWarehouse",
                "A_Role"
        };
    }

    @Override
    public Class<?>[] types() {
        return new Class<?>[] {
                ActionType.class, PuertoRicoConstants.BuildingType.class, Integer.class, Integer.class,
                PuertoRicoConstants.Crop.class, Integer.class,
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
        PuertoRicoGameState prs = (PuertoRicoGameState) state;

        Object[] retValue = new Object[names().length];
        if (action instanceof Build build) {
            retValue[0] = ActionType.BUILD;
            retValue[1] = build.type;
            retValue[2] = build.type.vp;
            retValue[3] = build.cost;
            setCropFeaturesFromBuilding(prs, playerID, build.type, retValue);
        } else if (action instanceof BuildQuarry){
            retValue[0] = ActionType.BUILD_QUARRY;
            retValue[4] = PuertoRicoConstants.Crop.QUARRY; // Quarry does not produce crops
            retValue[5] = 0; // Quarry does not produce crops
            retValue[6] = 1;
        } else if (action instanceof DiscardGoodsExcept discard){
            retValue[0] = ActionType.DISCARD_GOODS_EXCEPT;
            setCropFeatures(prs, playerID, discard.crop, retValue);
            retValue[9] = 1;
        } else if (action instanceof DrawPlantation dp) {
            retValue[0] = ActionType.DRAW_PLANTATION;
            setCropFeatures(prs, playerID, dp.crop, retValue);
        } else if (action instanceof GainCrop gc) {
            retValue[0] = ActionType.GAIN_CROP;
            setCropFeatures(prs, playerID, gc.crop, retValue);
        } else if (action instanceof OccupyBuilding ob) {
            retValue[0] = ActionType.OCCUPY_BUILDING;
            retValue[1] = ob.building;
            setCropFeaturesFromBuilding(prs, playerID, ob.building, retValue);
        } else if (action instanceof OccupyPlantation op) {
            retValue[0] = ActionType.OCCUPY_PLANTATION;
            int occupiedPlantations = prs.getPlayerBoard(playerID).getPlantations().stream()
                .filter(p -> p.isOccupied() && p.crop == op.crop).toList().size();
            boolean hasProductionCapacity = op.crop == CORN || prs.getPlayerBoard(playerID).getBuildings().stream()
                    .anyMatch(b -> b.buildingType.crop == op.crop && b.getOccupation() - occupiedPlantations > 0);
            setCropFeatures(prs, playerID, op.crop, retValue);
            retValue[6] = hasProductionCapacity ? 1 : 0; // If the player has a building that can produce this crop
        } else if (action instanceof SelectRole sr) {
            retValue[0] = ActionType.SELECT_ROLE;
            retValue[10] = sr.role;
        } else if (action instanceof Sell sc) {
            retValue[0] = ActionType.SELL;
            setCropFeatures(prs, playerID, sc.goodSold, retValue);
            retValue[3] = -sc.salesPrice;
        } else if (action instanceof ShipCargo sg) {
            retValue[0] = ActionType.SHIP_CARGO;
            setCropFeatures(prs, playerID, sg.cargo, retValue);
            retValue[4] = sg.amountToShip;
        } else if (action instanceof WarehouseStorage ws) {
            retValue[0] = ActionType.WAREHOUSE_STORAGE;
            setCropFeatures(prs, playerID, ws.storedCrop, retValue);
        }
        // then check the numeric features are no null
        for (int i = 0; i < retValue.length; i++) {
            if (retValue[i] == null) {
                if (types()[i] == Integer.class) {
                    retValue[i] = 0; // Default value for Integer
                } else if (types()[i] == Boolean.class) {
                    retValue[i] = false; // Default value for Boolean
                } else if (types()[i] == Double.class) {
                    retValue[i] = 0.0; // Default value for Double
                }
            }
        }
        return retValue;
    }

    private void setCropFeatures(PuertoRicoGameState prs, int playerID, PuertoRicoConstants.Crop crop, Object[] retValue) {
        if (crop == null) return;
        retValue[4] = crop;
        if (crop == PuertoRicoConstants.Crop.QUARRY) {
            return;
        }
        retValue[5] = crop.price;
        retValue[7] = prs.soldInMarket.contains(crop) ? 1 : 0; // If the crop is in the market
        retValue[8] = prs.getSupplyOf(crop);
        retValue[9] = prs.getPlayerBoard(playerID).getStoresOf(crop);
    }

    private void setCropFeaturesFromBuilding(PuertoRicoGameState prs, int playerID, PuertoRicoConstants.BuildingType type, Object[] retValue) {
        if (type == null || type.crop == null) return;
        int colonists = prs.getPlayerBoard(playerID).getUnassignedColonists();
        int plantations = prs.getPlayerBoard(playerID).getPlantationsOf(type.crop);
        int occupiedPlantations = prs.getPlayerBoard(playerID).getPlantations().stream().filter(p -> p.isOccupied() && p.crop == type.crop).toList().size();
        retValue[4] = type.crop;
        retValue[5] = type.crop.price;
        retValue[6] = Math.min(type.capacity, occupiedPlantations + Math.min(colonists, plantations));
        retValue[7] = prs.soldInMarket.contains(type.crop) ? 1 : 0; // If the crop is in the market
        retValue[8] = prs.getSupplyOf(type.crop);
        retValue[9] = prs.getPlayerBoard(playerID).getStoresOf(type.crop);
    }

}
