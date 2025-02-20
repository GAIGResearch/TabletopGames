package core.components;

import core.properties.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Hash;
import core.CoreConstants.ComponentType;

import java.util.*;

public abstract class Component {
    private static int ID = 0;  // All components receive a unique and final ID from this always increasing counter

    protected transient final int componentID;  // Unique ID of this component
    protected final ComponentType type;  // Type of this component
    protected HashMap<Integer, Property> properties;  // Maps between integer key for the property and the property object
    protected transient int ownerId = -1;  // By default belongs to the game
    protected String componentName;  // Name of this component

    public Component(ComponentType type, String name) {
        this.componentID = ID++;
        this.type = type;
        this.componentName = name;
        this.properties = new HashMap<>();
    }

    public Component(ComponentType type) {
        this.componentID = ID++;
        this.type = type;
        this.componentName = type.toString();
        this.properties = new HashMap<>();
    }

    protected Component(ComponentType type, String name, int componentID) {
        this.componentID = componentID;
        this.type = type;
        this.componentName = name;
        this.properties = new HashMap<>();
    }

    protected Component(ComponentType type, int componentID) {
        this.componentID = componentID;
        this.type = type;
        this.componentName = type.toString();
        this.properties = new HashMap<>();
    }

    /**
     * To be implemented by subclass, all components should be able to create copies of themselves.
     * @return - a new Component with the same properties.
     */
    public abstract Component copy();
    public Component copy(int playerId) { return copy(); }

    /**
     * Get and set the type of this component.
     */
    public ComponentType getType()                   {
        return this.type;
    }

    /**
     * Get number of properties for this component.
     * @return - int, size of properties map.
     */
    public int getNumProperties()
    {
        return properties.size();
    }

    /**
     * Get and set the ID of the player who owns the deck (-1, the game's, by default).
     * @return - int, owner ID
     */
    public int getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Get the ID of this component.
     * @return - component ID.
     */
    public int getComponentID() {
        return componentID;
    }

    /**
     * @return name of this component.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Sets the name of this component.
     * @param componentName - new name for this component.
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Get the full map of properties.
     * @return - mapping from property integer key to property objects.
     */
    public Map<Integer, Property> getProperties() {
        return properties;
    }

    /**
     * Gets a property from the properties.
     * @param propId id of the property to look for
     * @return the property value. Null if it doesn't exist.
     */
    public Property getProperty(int propId)
    {
        return properties.get(propId);
    }

    public Property getProperty(String hashString) {
        return properties.get(Hash.GetInstance().hash(hashString));
    }

    /**
     * Adds a property with an id and a Property object
     * @param prop property to add
     */
    public void setProperty(Property prop)
    {
        properties.put(prop.getHashKey(), prop);
    }

    public void setProperties(Map<Integer, Property> props) {
        for (Property p: props.values()) {
            setProperty(p);
        }
    }

    public static Component parseComponent(Component c, JSONObject obj) {
        return parseComponent(c, obj, new HashSet<>());
    }

    /**
     * Parses a Component object from a JSON object.
     * @param obj - JSON object to parse.
     * @return new Component object with properties as defined in JSON.
     */
    public static Component parseComponent(Component c, JSONObject obj, Set<String> ignoreKeys)
    {
        for(Object o : obj.keySet())
        {
            String key = (String)o;
            if (ignoreKeys.contains(key)) continue;

            if(obj.get(key) instanceof JSONArray) {
                JSONArray value = (JSONArray) obj.get(key);
                String type = (String) value.get(0);

                Property prop = null;
                if (type.contains("[]"))  // Array
                {
                    JSONArray values = (JSONArray) value.get(1);

                    if (type.contains("String")) {
                        prop = new PropertyStringArray(key, values);
                    } else if (type.contains("Integer")) {
                        prop = new PropertyIntArray(key, values);
                    } else if (type.contains("Long")) {
                        prop = new PropertyLongArray(key, values);
                    }
                    //More types of arrays to come.
                } else if (type.contains("<>")) {  // We've got a list!
                    JSONArray values = (JSONArray) value.get(1);

                    if (type.contains("Integer")) {
                        prop = new PropertyIntArrayList(key, values);
                    } else if (type.contains("Long")) {
                        prop = new PropertyLongArrayList(key, values);
                    }
                } else {
                    if (type.contains("String")) {
                        prop = new PropertyString(key, (String) value.get(1));
                    } else if (type.contains("Color")) {
                        prop = new PropertyColor(key, (String) value.get(1));
                    } else if (type.contains("Vector2D")) {
                        prop = new PropertyVector2D(key, (JSONArray) value.get(1));
                    } else if (type.contains("Boolean")) {
                        prop = new PropertyBoolean(key, (boolean) value.get(1));
                    } else if (type.contains("Integer")) {
                        prop = new PropertyInt(key, ((Long) value.get(1)).intValue());
                    } else if (type.contains("Long")) {
                        prop = new PropertyLong(key, (long) value.get(1));
                    }
                }
                if (prop != null) {
                    c.setProperty(prop);
                }
            }
        }

        return c;
    }

    /**
     * Copies super class variables in given subclass instance.
     * @param copyTo - subclass component instance
     */
    public void copyComponentTo(Component copyTo)
    {
        copyTo.properties.clear();
        for (int prop_key : this.properties.keySet()) {
            Property newProp = this.properties.get(prop_key).copy();
            copyTo.setProperty(newProp);
        }
        copyTo.ownerId = ownerId;
        copyTo.componentName = componentName;
    }

    @Override
    public String toString() {
        return "Component{" +
                "componentID=" + componentID +
                ", type=" + type +
                ", ownerId=" + ownerId +
                ", componentName='" + componentName + '\'' +
                ", properties=" + properties +
                '}';
    }

    public String toString(int playerId) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Component component)) return false;
        return componentID == component.componentID;
    }

    @Override
    public int hashCode() {
        return componentID;
    }
}
