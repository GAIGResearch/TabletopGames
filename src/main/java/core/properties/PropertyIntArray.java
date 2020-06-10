package core.properties;

import org.json.simple.JSONArray;

import java.util.Arrays;

public class PropertyIntArray extends Property
{
    private int[] values;

    public PropertyIntArray(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new int[values.size()];
        for(int i = 0; i < values.size(); i++) {
            this.values[i] = ((Long) values.get(i)).intValue();
        }
    }

    private PropertyIntArray(String hashString, int hashKey, int[] values)
    {
        super(hashString, hashKey);
        this.values = new int[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public int[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    @Override
    public boolean equals(Object other)
    {
       if(other instanceof PropertyIntArray) {
           PropertyIntArray psto = (PropertyIntArray)(other);
           if (psto.values.length == this.values.length)
           {
               for(int i = 0; i < values.length; i++) {
                   if (! (this.values[i] == psto.values[i])) {
                       return false;
                   }
               }
           } else return false;

       } else return false;

       return true;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyIntArray(hashString, hashKey, values);
    }

}
