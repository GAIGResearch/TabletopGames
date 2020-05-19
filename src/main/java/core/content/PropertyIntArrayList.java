package core.content;

import org.json.simple.JSONArray;
import utilities.Hash;

import java.util.ArrayList;

public class PropertyIntArrayList extends Property
{
    private ArrayList<Integer> values;

    public PropertyIntArrayList(ArrayList<Integer> values)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    public PropertyIntArrayList(String key, JSONArray values)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.values = new ArrayList<>();
        for (Object value : values) {
            this.values.add((int) value);
        }

    }

    public PropertyIntArrayList(String key, int hashKey, ArrayList<Integer> values)
    {
        this.hashString = key;
        this.hashKey = hashKey;
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

    public boolean equals(Object other)
    {
       if(other instanceof PropertyIntArrayList)
       {
           PropertyIntArrayList psto = (PropertyIntArrayList)(other);
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
        return new PropertyIntArrayList(hashString, hashKey, values);
    }

}
