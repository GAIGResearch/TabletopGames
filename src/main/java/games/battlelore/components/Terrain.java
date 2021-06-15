package games.battlelore.components;

import core.components.Component;
import utilities.Utils;

public class Terrain extends Component
{
    public Terrain(Utils.ComponentType type, String name)
    {
        super(type, name);
    }

    @Override
    public Component copy() {
        return null;
    }
}
