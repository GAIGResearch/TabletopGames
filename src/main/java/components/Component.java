package components;

import content.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Hash;
import utilities.Utils.ComponentType;

import java.util.HashMap;

public abstract class Component {
    protected ComponentType type;
    protected HashMap<Integer, Property> properties;

    public ComponentType getType()                   { return this.type; }
    public void          setType(ComponentType type) { this.type = type; }


    public int getNumProperties()
    {
        return properties.size();
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

    /**
     * Adds a property with an id and a Property object
     * @param propId ID of the property
     * @param prop property to add
     */
    public void setProperty(int propId, Property prop)
    {
        properties.put(propId, prop);
    }

    /**
     * Parses a Component object from a JSON object.
     * @param obj - JSON object to parse.
     * @return new Component object with properties as defined in JSON.
     */
    protected static Component parseComponent(Component c, JSONObject obj)
    {
        for(Object o : obj.keySet())
        {
            String key = (String)o;

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
                c.setProperty(Hash.GetInstance().hash(prop.getHashString()), prop);
            }
        }

        return c;
    }

    public void copyComponentTo(Component copyTo)
    {
        for(int prop_key : this.properties.keySet())
        {
            Property newProp = this.properties.get(prop_key).copy();
            copyTo.setProperty(prop_key, newProp);
        }

        copyTo.type = this.type;
    }

}
