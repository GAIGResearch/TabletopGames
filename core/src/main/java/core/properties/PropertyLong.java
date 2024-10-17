package core.properties;

public class PropertyLong extends Property
{
    public long value;

    public PropertyLong(String hashString, long value)
    {
        super(hashString);
        this.value = value;
    }

    public PropertyLong(String hashString, int hashKey, long value)
    {
        super(hashString, hashKey);
        this.value = value;
    }


    @Override
    public String toString() {
        return ""+value;
    }

    @Override
    public boolean equals(Object other)
    {
       if(other instanceof PropertyLong)
           return value == ((PropertyLong)(other)).value;
       return false;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyLong(hashString, hashKey, value);
    }

}
