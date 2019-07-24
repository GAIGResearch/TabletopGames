package components;

import utilities.Utils.ComponentType;

public abstract class Component {
    protected ComponentType type;

    public ComponentType getType()                   { return this.type; }
    public void          setType(ComponentType type) { this.type = type; }
}
