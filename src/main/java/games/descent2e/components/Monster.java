package games.descent2e.components;

import core.properties.Property;
import core.properties.PropertyStringArray;
import utilities.Vector2D;

import java.util.Map;

import static games.descent2e.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)
    private Vector2D adjacentLocation;

    public Monster() {
        super("Monster");
    }

    protected Monster(String name, int ID) {
        super(name, ID);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    @Override
    public Monster copy() {
        Monster copy = new Monster(componentName, componentID);
        copy.orientation = orientation;

        if (adjacentLocation != null) {
            copy.adjacentLocation = adjacentLocation.copy();
        } else {
            copy.adjacentLocation = null;
        }
        super.copyComponentTo(copy);
        return copy;
    }

    public Monster copyNewID() {
        Monster copy = new Monster();
        copy.orientation = orientation;

        if (adjacentLocation != null){
            copy.adjacentLocation = adjacentLocation.copy();
        } else {
            copy.adjacentLocation = null;
        }

        super.copyComponentTo(copy);
        return copy;
    }

    public Vector2D getAdjacentLocation() {
        return adjacentLocation;
    }

    public void setAdjacentLocation(Vector2D adjacentLocation) {
        this.adjacentLocation = adjacentLocation;
    }
}
