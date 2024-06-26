package core.properties;

import org.json.simple.JSONArray;

import java.util.Arrays;

public class PropertyLongArray extends Property
{
    private long[] values;

    public PropertyLongArray(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new long[values.size()];
        for(int i = 0; i < values.size(); i++)
        {
            this.values[i] = (long) values.get(i);
        }

    }

    public PropertyLongArray(String hashString, int hashKey, long[] values)
    {
        super(hashString, hashKey);
        this.values = new long[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public long[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    @Override
    public boolean equals(Object other)
    {
       if (other instanceof PropertyLongArray)
       {
           PropertyLongArray psto = (PropertyLongArray)(other);
           if (psto.values.length == this.values.length)
           {
               for (int i = 0; i < values.length; i++) {
                   if (! (this.values[i] == psto.values[i]))
                       return false;
               }
           } else return false;

       } else return false;

       return true;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyLongArray(hashString, hashKey, values);
    }

}
