package games.descent.components;

import core.properties.Property;
import core.properties.PropertyInt;

import java.util.HashMap;

import static games.descent.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)

    public Monster(String name, HashMap<Integer, Property> props) {
        super(name);
        properties.clear();
        properties.putAll(props);

        this.movePoints = ((PropertyInt)getProperty(movementHash)).value;
        this.hp = ((PropertyInt)getProperty(healthHash)).value;

        tokenType = "Monster";
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
        super.copyComponentTo(copy);
        return copy;
    }
}
