package core.content;

import org.json.simple.JSONArray;
import utilities.Hash;

import java.util.Arrays;

public class PropertyStringArray extends Property
{
    private String[] values;

    public PropertyStringArray (String[] values)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new String[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] = values[i];

    }

    public PropertyStringArray(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new String[values.size()];
        for(int i =0; i< values.size(); ++i)
        {
            this.values[i] = (String) values.get(i);
        }

    }

    public PropertyStringArray(String key, int hashKey, String[] values)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.values = new String[values.length];
        for(int i =0; i< values.length; ++i)
            this.values[i] = values[i];
    }

    public String[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    public boolean equals(Object other)
    {
       if(other instanceof PropertyStringArray)
       {
           PropertyStringArray psto = (PropertyStringArray)(other);
           if (psto.values.length == this.values.length)
           {
               for(int i =0; i< values.length; ++i) {
                   if (! this.values[i].equals(psto.values[i]))
                       return false;
               }
           }else return false;

       }else return false;

       return true;
    }

    public Property copy()
    {
        return new PropertyStringArray(hashString, hashKey, values);
    }

}
