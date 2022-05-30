package games.descent2e.components;

import core.components.Counter;
import core.properties.Property;
import core.properties.PropertyInt;

import java.util.HashMap;

import static games.descent2e.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)

    public Monster(String name, HashMap<Integer, Property> props) {
        super(name);
        properties.clear();
        properties.putAll(props);

        int mp = ((PropertyInt)getProperty(movementHash)).value;
        this.setAttribute(Attribute.MovePoints, new Counter(0, 0, mp, "Move points"));
        int hp = ((PropertyInt)getProperty(healthHash)).value;
        this.setAttribute(Attribute.Health, new Counter(hp, 0, hp, "Health"));

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
