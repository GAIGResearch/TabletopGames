package core.properties;

import utilities.Hash;

public class PropertyString extends Property
{
    public String value;

    public PropertyString (String value)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyString (String key, String value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyString (String key, int hashKey, String value)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }

    public boolean equals(Object other)
    {
       if(other instanceof PropertyString)
           return value.equals(((PropertyString)(other)).value);
       return false;
    }

    public Property copy()
    {
        return new PropertyString(hashString, hashKey, value);
    }

}
