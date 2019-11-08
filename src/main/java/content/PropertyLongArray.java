package content;

import org.json.simple.JSONArray;
import utilities.Hash;

import java.util.Arrays;

public class PropertyLongArray extends Property
{
    private long[] values;

    public PropertyLongArray(long[] values)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new long[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] =  values[i];

    }

    public PropertyLongArray(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new long[values.size()];
        for(int i =0; i< values.size(); ++i)
        {
            this.values[i] = (long) values.get(i);
        }

    }

    public PropertyLongArray(String key, int hashKey, long[] values)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.values = new long[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] = values[i];
    }

    public long[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    public boolean equals(Object other)
    {
       if(other instanceof PropertyLongArray)
       {
           PropertyLongArray psto = (PropertyLongArray)(other);
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
        return new PropertyLongArray(hashString, hashKey, values);
    }

}
