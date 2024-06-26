package core.properties;

import org.json.simple.JSONArray;

import java.util.Arrays;

public class PropertyStringArray extends Property
{
    private String[] values;

    public PropertyStringArray(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new String[values.size()];
        for(int i = 0; i < values.size(); i++)
        {
            this.values[i] = (String) values.get(i);
        }

    }

    public PropertyStringArray(String hashString, int hashKey, String[] values)
    {
        super(hashString, hashKey);
        this.values = new String[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public String[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    @Override
    public boolean equals(Object other)
    {
       if(other instanceof PropertyStringArray)
       {
           PropertyStringArray psto = (PropertyStringArray)(other);
           if (psto.values.length == this.values.length)
           {
               for(int i = 0; i < values.length; i++) {
                   if (! this.values[i].equals(psto.values[i]))
                       return false;
               }
           }else return false;

       }else return false;

       return true;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyStringArray(hashString, hashKey, values);
    }

}
