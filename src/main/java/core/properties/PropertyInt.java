package core.properties;


public class PropertyInt extends Property
{
    public int value;

    public PropertyInt(String hashString, int value)
    {
        super(hashString);
        this.value = value;
    }

    private PropertyInt(String hashString, int hashKey, int value)
    {
        super(hashString, hashKey);
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

    @Override
    protected Property _copy()
    {
        return new PropertyInt(hashString, hashKey, value);
    }

}
