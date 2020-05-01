package core.content;

import utilities.Hash;

public class PropertyLong extends Property
{
    public long value;

    public PropertyLong(long value)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyLong(String key, long value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyLong(String key, int hashKey, long value)
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
       if(other instanceof PropertyLong)
           return value == ((PropertyLong)(other)).value;
       return false;
    }

    public Property copy()
    {
        return new PropertyLong(hashString, hashKey, value);
    }

}
