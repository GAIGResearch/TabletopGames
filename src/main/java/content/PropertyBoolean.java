package content;

import utilities.Hash;

public class PropertyBoolean extends Property
{
    public Boolean value;

    public PropertyBoolean(boolean value)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyBoolean(String key, boolean value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyBoolean(String key, int hashKey, boolean value)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.value = value;
    }


    @Override
    public String toString() {
        return ""+value;
    }

    public boolean equals(Object other)
    {
       if(other instanceof PropertyBoolean)
           return value.equals(((PropertyBoolean)(other)).value);
       return false;
    }

    public Property copy()
    {
        return new PropertyBoolean(hashString, hashKey, value);
    }

}
