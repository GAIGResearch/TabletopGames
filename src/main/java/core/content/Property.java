package core.content;

public abstract class Property
{
    protected String hashString;
    protected int hashKey;

    public String getHashString() {return hashString;}
    public int getHashKey() {return  hashKey;}


    public abstract String toString();

    public abstract boolean equals(Object other);

    public abstract Property copy();

}
