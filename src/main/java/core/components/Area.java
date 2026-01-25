package core.components;

import core.CoreConstants;
import core.interfaces.IComponentContainer;

import java.util.*;

/**
 * An Area is a collection of components such as Decks, Token, Dices, Cards and Boards, mapping to their IDs.
 */
public class Area extends Component implements IComponentContainer<Component> {

    // Collection of components stored in this area, mapping to their IDs
    protected HashMap<Integer, Component> components;

    public Area(int owner, String name) {
        super(CoreConstants.ComponentType.AREA, "");
        this.components = new HashMap<>();
        this.ownerId = owner;
    }

    private Area(int owner, String name, int ID) {
        super(CoreConstants.ComponentType.AREA, "", ID);
        this.components = new HashMap<>();
        this.ownerId = owner;
    }

    public Area copy() {
        Area new_area = new Area(ownerId, componentName, componentID);
        new_area.components = new HashMap<>();
        for (Map.Entry<Integer, Component> c: this.components.entrySet()){
            new_area.components.put(c.getKey(), c.getValue().copy());
        }
        copyComponentTo(new_area);
        return new_area;
    }

    /**
     * This is used to avoid a full recursive copy of all the contents of an area
     * This can be useful when we need for other reasons to copy those locally, and it
     * is easier to put them into the Area once this is done.
     *
     * The main use case for this is in AbstractGameState.copy() and the creation of allComponents.
     *
     * @return An empty Area that has the same ComponentID as the original
     */
    public Area emptyCopy() {
        return new Area(ownerId, componentName, componentID);
    }

    /**
     * Clears the collection of components.
     */
    public void clear() {
        components.clear();
    }

    /**
     * Retrieve the collection of components in this area.
     * @return - HashMap, components mapped to their IDs
     */
    public HashMap<Integer, Component> getComponentsMap() {
        return this.components;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }

    @Override
    public List<Component> getComponents() {
        return new ArrayList<>(components.values());
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
        if (component == null) return;

        this.components.put(component.getComponentID(), component);
        if (component instanceof IComponentContainer) {
            for (Component nestedC : ((IComponentContainer<?>) component).getComponents()) {
                if (nestedC != null) {
                    putComponent(nestedC);
                }
            }
        }
    }

    public void removeComponent(Component component) {
        if (component instanceof Deck || component instanceof Area)
            throw new IllegalArgumentException("Not yet implemented for Decks or Areas");
        if (components.containsKey(component.componentID)) {
            this.components.remove(component.componentID);
        } else {
            throw new IllegalArgumentException("Cannot remove Component as it is not here : " + component.componentID);
        }
    }

    /**
     * Adds all components in a list to the collection, using their own component IDs as the keys in the map.
     * @param components - list of components to add to the collection.
     */
    public void putComponents(List<? extends Component> components) {
        for (Component c: components) {
            putComponent(c);
        }
    }

    /**
     * Returns the size of this area, i.e. number of components in it.
     * @return - size of components map.
     */
    public int size() {
        return components.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Area) {
            Area other = (Area) o;
            return componentID == other.componentID && other.components.equals(components);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, components);
    }
}
