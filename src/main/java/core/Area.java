package core;

import components.*;
import utilities.Utils.ComponentType;

import java.util.HashSet;

public class Area {
    protected HashSet<Component> components;

    public Area() {
        components = new HashSet<>();
    }

    public Area(HashSet<Component> components) {
        this.components = new HashSet<>(components);
    }

    public Area copy() {
        Area new_area = new Area();
        new_area.components = new HashSet<>(this.components);
        return new_area;
    }

    /**
     * get all components
     */
    public HashSet<Component> getComponents() { return components; }


    /**
     * get all components of given type
     */
    public HashSet<Component> getComponentsOfType(ComponentType type) {
        HashSet<Component> new_components = new HashSet<>();
        for (Component component:components) {
            if (component.getType() == type)
                new_components.add(component);
        }
        return new_components;
    }

    /**
     * add new component with given type
     */
    public void addComponentOfType(Component component, ComponentType type) {
        component.setType(type);
        components.add(component);
    }
}
