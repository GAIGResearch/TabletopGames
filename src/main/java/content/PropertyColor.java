package content;

import utilities.Hash;
import utilities.Utils;

import java.awt.*;

public class PropertyColor extends Property
{
    public String valueStr;
    private Color value;

    public PropertyColor (String hashKey, String valStr)
    {
        this.hashString = hashKey;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = Utils.stringToColor(valStr);
        this.valueStr = valStr;
    }

    public PropertyColor(String key, int hashKey, Color value)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.value = value;
        this.valueStr = value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof PropertyColor)
            return value.equals(((PropertyColor)(other)).value);
        return false;
    }

    @Override
    public Property copy() {
        return new PropertyColor(hashString, hashKey, value);
    }
}
