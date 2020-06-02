package core.properties;

import utilities.Utils;

import java.awt.*;
import java.util.Objects;

public class PropertyColor extends Property
{
    public String valueStr;
    private Color value;

    public PropertyColor(String hashString, String valStr)
    {
        super(hashString);
        this.value = Utils.stringToColor(valStr);
        this.valueStr = valStr;
    }

    private PropertyColor(String hashString, int hashKey, Color value, String valueStr)
    {
        super(hashString, hashKey);
        this.value = value;
        this.valueStr = valueStr;
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
    protected Property _copy() {
        return new PropertyColor(hashString, hashKey, value, valueStr);
    }
}
