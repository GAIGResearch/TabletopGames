package core.properties;

import org.json.simple.JSONArray;

import java.util.ArrayList;

public class PropertyLongArrayList extends Property
{
    private ArrayList<Long> values;

    public PropertyLongArrayList(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new ArrayList<>();
        for (Object value : values) {
            this.values.add((long) value);
        }

    }

    public PropertyLongArrayList(String hashString, int hashKey, ArrayList<Long> values)
    {
        super(hashString, hashKey);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    public ArrayList<Long> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object other)
    {
       if (other instanceof PropertyLongArrayList)
       {
           PropertyLongArrayList psto = (PropertyLongArrayList)(other);
           if (psto.values.size() == this.values.size())
           {
               for(int i = 0; i < values.size(); i++) {
                   if (! (this.values.get(i).equals(psto.values.get(i))))
                       return false;
               }
           } else return false;

       } else return false;

       return true;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyLongArrayList(hashString, hashKey, values);
    }

}
