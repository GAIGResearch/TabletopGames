package core.properties;

import org.json.simple.JSONArray;
import utilities.Hash;

import java.util.ArrayList;

public class PropertyLongArrayList extends Property
{
    private ArrayList<Long> values;

    public PropertyLongArrayList(ArrayList<Long> values)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    public PropertyLongArrayList(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new ArrayList<>();
        for (Object value : values) {
            this.values.add((long) value);
        }

    }

    public PropertyLongArrayList(String key, int hashKey, ArrayList<Long> values)
    {
        this.hashString = key;
        this.hashKey = hashKey;
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

    public boolean equals(Object other)
    {
       if(other instanceof PropertyLongArrayList)
       {
           PropertyLongArrayList psto = (PropertyLongArrayList)(other);
           if (psto.values.size() == this.values.size())
           {
               for(int i =0; i< values.size(); ++i) {
                   if (! (this.values.get(i).equals(psto.values.get(i))))
                       return false;
               }
           }else return false;

       }else return false;

       return true;
    }

    public Property copy()
    {
        return new PropertyLongArrayList(hashString, hashKey, values);
    }

}
