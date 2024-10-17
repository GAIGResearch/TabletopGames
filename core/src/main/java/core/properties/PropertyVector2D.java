package core.properties;

import org.json.simple.JSONArray;
import utilities.Vector2D;

import java.util.Objects;

public class PropertyVector2D extends Property
{
    public Vector2D values;

    public PropertyVector2D(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new Vector2D((int)(long)values.get(0), (int)(long)values.get(1));
    }

    public PropertyVector2D(String hashString, Vector2D v)
    {
        super(hashString);
        this.values = new Vector2D(v.getX(), v.getY());
    }

    public PropertyVector2D(String hashString, int hashKey, Vector2D v)
    {
        super(hashString, hashKey);
        this.values = new Vector2D(v.getX(), v.getY());
    }

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyVector2D that = (PropertyVector2D) o;
        return Objects.equals(values, that.values);
    }

    @Override
    protected Property _copy()
    {
        return new PropertyVector2D(hashString, hashKey, values);
    }

}
