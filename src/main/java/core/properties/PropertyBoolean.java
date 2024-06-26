package core.properties;


public class PropertyBoolean extends Property
{
    public Boolean value;

    public PropertyBoolean(boolean value)
    {
        super("");
        this.value = value;
    }

    public PropertyBoolean(String hashString, boolean value)
    {
        super(hashString);
        this.value = value;
    }

    private PropertyBoolean(String hashString, int hashKey, boolean value)
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
       if(other instanceof PropertyBoolean)
           return value.equals(((PropertyBoolean)(other)).value);
       return false;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyBoolean(hashString, hashKey, value);
    }

}
