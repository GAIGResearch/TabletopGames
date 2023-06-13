package games.puertorico.components;

import games.puertorico.PuertoRicoConstants;

public class ProductionBuilding extends Building {

    public final PuertoRicoConstants.Crop cropType;

    public ProductionBuilding(PuertoRicoConstants.BuildingType type) {
        super(type);
        switch (type) {
            case SUGAR_MILL:
            case SMALL_SUGAR_MILL:
                this.cropType = PuertoRicoConstants.Crop.SUGAR;
                break;
            case SMALL_INDIGO_PLANT:
            case INDIGO_PLANT:
                this.cropType = PuertoRicoConstants.Crop.INDIGO;
                break;
            case COFFEE_ROASTER:
                this.cropType = PuertoRicoConstants.Crop.COFFEE;
                break;
            case TOBACCO_STORAGE:
                this.cropType = PuertoRicoConstants.Crop.TOBACCO;
                break;
            default:
                throw new IllegalArgumentException("Invalid building type for production building: " + type);
        }
    }

    private ProductionBuilding(ProductionBuilding building) {
        super(building);
        this.cropType = building.cropType;
    }

    @Override
    public ProductionBuilding copy() {
        return new ProductionBuilding(this);
    }
}
