package core.content;

import org.json.simple.JSONArray;
import utilities.Hash;
import utilities.Vector2D;

public class PropertyVector2D extends Property
{
    public Vector2D values;

//    public PropertyVector2D(String[] values)
//    {
//        this.hashString = "";
//        this.hashKey = Hash.GetInstance().hash(hashString);
//        this.values = new String[values.length];
//        for(int i =0; i< values.length; ++i)
//            this.values[i] = values[i];
//
//    }

    public PropertyVector2D(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new Vector2D((int)(long)values.get(0), (int)(long)values.get(1));
    }

    public PropertyVector2D(String key, int hashKey, Vector2D v)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.values = new Vector2D(v.getX(), v.getY());
    }


    @Override
    public String toString() {
        return values.toString();
    }

    public boolean equals(Object other)
    {
        return this.values.equals(((PropertyVector2D)other).values);
    }

    public Property copy()
    {
        return new PropertyVector2D(hashString, hashKey, values);
    }

}
