package core.components;

import java.util.HashSet;

public interface IToken {

    /**
     * Creates a copy of this object.
     * @return a copy of the IToken.
     */
    IToken copy();

    HashSet<Integer> getOwner();

    void setOwner(HashSet<Integer> owner);

    int getOccurenceLimit();

    void setOccurenceLimit(int occurenceLimit);

    String getNameID();
}
