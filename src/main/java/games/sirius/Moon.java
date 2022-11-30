package games.sirius;


import core.components.Component;
import core.properties.Property;
import utilities.Utils;

import java.util.HashMap;

enum MoonType {
    MINING, TRADING
}

public class Moon extends Component {

    public Moon(String name, MoonType type) {
        super(Utils.ComponentType.AREA, name);
        this.type = type;
    }

    public final MoonType type;

    @Override
    public Component copy() {
        return this; // immutable
    }

    @Override
    public void setProperty(Property prop) {
        throw new AssertionError("No Properties allowed for immutability");
    }

    public void setProperties(HashMap<Integer, Property> props) {
        throw new AssertionError("No Properties allowed for immutability");
    }

}

