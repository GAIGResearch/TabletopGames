package core.properties;

import org.json.simple.JSONArray;
import utilities.Hash;

import java.util.Arrays;

public class PropertyIntArray extends Property
{
    private int[] values;

    public PropertyIntArray(int[] values)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new int[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] =  values[i];

    }

    public PropertyIntArray(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new int[values.size()];
        for(int i =0; i< values.size(); ++i)
        {
            this.values[i] = ((Long) values.get(i)).intValue();
        }

    }

    public PropertyIntArray(String key, int hashKey, int[] values)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.values = new int[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] = values[i];
    }

    public int[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    public boolean equals(Object other)
    {
       if(other instanceof PropertyIntArray)
       {
           PropertyIntArray psto = (PropertyIntArray)(other);
           if (psto.values.length == this.values.length)
           {
               for(int i =0; i< values.length; ++i) {
                   if (! (this.values[i] == psto.values[i]))
                       return false;
               }
           }else return false;

       }else return false;

       return true;
    }

    public Property copy()
    {
        return new PropertyIntArray(hashString, hashKey, values);
    }

}
