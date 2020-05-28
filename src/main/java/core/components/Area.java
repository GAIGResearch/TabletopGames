package core.components;

import utilities.Utils;

import java.util.HashMap;
import java.util.List;

/**
 * An Area is a collection of components such as Decks, Token, Dices, Cards and Boards, mapping to their IDs.
 */
public class Area<T extends Component> extends Component {

    // Collection of components stored in this area, mapping to their IDs
    protected HashMap<Integer, Component> components;

    public Area(int owner, String name) {
        super(Utils.ComponentType.AREA, "");
        this.components = new HashMap<>();
        this.ownerId = owner;
    }

    private Area(int owner, String name, int ID) {
        super(Utils.ComponentType.AREA, "", ID);
        this.components = new HashMap<>();
        this.ownerId = owner;
    }

    public Area copy() {
        Area new_area = new Area(ownerId, componentName, componentID);
        new_area.components = new HashMap<>();
        new_area.components.putAll(this.components);
        copyComponentTo(new_area);
        return new_area;
    }

    /**
     * Retrieve the collection of components in this area.
     * @return - HashMap, components mapped to their IDs
     */
    public HashMap<Integer, Component> getComponents() {
        return this.components;
    }

    /**
     * Retrieve a component by its id key.
     * @param key - key to look for in the map.
     * @return - component corresponding to the given key.
     */
    public Component getComponent(Integer key) {
        return this.components.get(key);
    }

    /**
     * Adds a component to the collection.
     * @param key - key for the component.
     * @param component - component to add to the collection.
     */
    public void putComponent(Integer key, Component component) {
        this.components.put(key, component);
    }

    /**
     * Adds a component to the collection, using its own component ID as the key in the map.
     * @param component - component to add to the collection.
     */
    public void putComponent(Component component) {
        this.components.put(component.getComponentID(), component);
        if (component instanceof Deck) {
            putComponents(((Deck) component).getComponents());
        }
    }

    public void putComponents(List<? extends Component> components) {
        for (Component c: components) {
            putComponent(c);
        }
    }

    public void putComponents(Area<T> area) {
        for (Component c: area.components.values()) {
            putComponent(c);
        }
    }

    public void clear() {
        this.components.clear();
    }
}
