package games.descent.components;

import core.properties.Property;
import core.properties.PropertyInt;
import utilities.Pair;

import java.util.HashMap;

import static games.descent.DescentConstants.*;

public class Monster extends Figure {

    int orientation;  // medium monsters might be vertical (0) or horizontal (1)
    Pair<Integer,Integer> size;

    public Monster(String name, HashMap<Integer, Property> props) {
        super(name);
        properties.clear();
        properties.putAll(props);

        this.movePoints = ((PropertyInt)getProperty(movementHash)).value;
        this.hp = ((PropertyInt)getProperty(healthHash)).value;
        // TODO: token type might not be set as monster
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

    public void setSize(int width, int height) {
        this.size = new Pair<>(width, height);
    }

    public Pair<Integer, Integer> getSize() {
        return size;
    }

    @Override
    public Monster copy() {
        Monster copy = new Monster(componentName, componentID);
        copy.xp = xp;
        copy.tokenType = tokenType;
        copy.movePoints = movePoints;
        copy.hp = hp;
        if (location != null) {
            copy.location = location.copy();
        }
        copy.nActionsExecuted = nActionsExecuted;
        copyComponentTo(copy);
        return copy;
    }
}
