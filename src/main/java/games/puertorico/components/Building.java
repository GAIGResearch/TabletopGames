package games.puertorico.components;

import core.CoreConstants;
import core.components.Component;
import games.puertorico.PuertoRicoConstants;

import java.util.Objects;

public class Building extends Component {

    public final PuertoRicoConstants.BuildingType buildingType;
    int occupation;
    boolean used = false;

    public Building(PuertoRicoConstants.BuildingType type) {
        super(CoreConstants.ComponentType.TOKEN, type.name());
        this.buildingType = type;
        this.occupation = 0;
    }

    protected Building(Building building) {
        super(CoreConstants.ComponentType.TOKEN, building.componentName, building.componentID);
        this.buildingType = building.buildingType;
        this.occupation = building.occupation;
        this.used = building.used;
    }

    public int getOccupation() {
        return occupation;
    }

    public void setUsed() {
        this.used = true;
    }

    public void refresh() {
        this.used = false;
    }

    public boolean hasBeenUsed() {
        return used;
    }

    public void setOccupation(int occupation) {
        this.occupation = occupation;
    }

    @Override
    public Building copy() {
        return new Building(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Building) {
            Building building = (Building) obj;
            return super.equals(building) && this.buildingType == building.buildingType && this.occupation == building.occupation &&
                    this.used == building.used;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), occupation, buildingType.ordinal(), used);
    }

    @Override
    public String toString() {
        return String.format("%s (Occ: %d)", componentName, occupation);
    }

    public static Building instantiate(PuertoRicoConstants.BuildingType type) {

        switch (type) {
            case SUGAR_MILL:
            case SMALL_SUGAR_MILL:
            case SMALL_INDIGO_PLANT:
            case INDIGO_PLANT:
            case COFFEE_ROASTER:
            case TOBACCO_STORAGE:
                return new ProductionBuilding(type);
            case GUILD_HALL:
            case RESIDENCE:
            case FORTRESS:
            case CITY_HALL:
            case CUSTOMS_HOUSE:
                return new VictoryBuilding(type);
            default:
                return new Building(type);
        }
    }
}
