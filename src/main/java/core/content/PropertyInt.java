package core.content;

import utilities.Hash;

public class PropertyInt extends Property
{
    public int value;

    public PropertyInt(int value)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyInt(String key, int value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public PropertyInt(String key, int hashKey, int value)
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
       if(other instanceof PropertyInt)
           return value == ((PropertyInt)(other)).value;
       return false;
    }

    public Property copy()
    {
        return new PropertyInt(hashString, hashKey, value);
    }

}
