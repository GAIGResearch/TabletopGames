package core.properties;

import org.json.simple.JSONArray;

import java.util.ArrayList;

public class PropertyIntArrayList extends Property
{
    private ArrayList<Integer> values;

    public PropertyIntArrayList(String hashString, JSONArray values)
    {
        super(hashString);
        this.values = new ArrayList<>();
        for (Object value : values) {
            this.values.add((int) value);
        }

    }

    private PropertyIntArrayList(String hashString, int hashKey, ArrayList<Integer> values)
    {
        super(hashString, hashKey);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    public ArrayList<Integer> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object other)
    {
       if(other instanceof PropertyIntArrayList)
       {
           PropertyIntArrayList psto = (PropertyIntArrayList)(other);
           if (psto.values.size() == this.values.size())
           {
               for (int i = 0; i < values.size(); i++) {
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
        return new PropertyIntArrayList(hashString, hashKey, values);
    }

}
