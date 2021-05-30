package games.battlelore.components;

import core.components.Component;
import core.interfaces.IComponentContainer;
import utilities.Utils;

import java.util.function.Function;

public class Unit extends Component
{
    public Unit(Utils.ComponentType type, String name) {
        super(type, name);
    }

    @Override
    public Component copy() {
        return null;
    }
}
