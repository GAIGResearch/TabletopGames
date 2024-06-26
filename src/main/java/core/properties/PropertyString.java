package core.properties;


public class PropertyString extends Property
{
    public String value;

    public PropertyString (String value)
    {
        super("");
        this.value = value;
    }

    public PropertyString (String hashString, String value)
    {
        super(hashString);
        this.value = value;
    }

    public PropertyString (String hashString, int hashKey, String value)
    {
        super(hashString, hashKey);
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other)
    {
       if(other instanceof PropertyString)
           return value.equals(((PropertyString)(other)).value);
       return false;
    }

    @Override
    protected Property _copy()
    {
        return new PropertyString(hashString, hashKey, value);
    }

}
