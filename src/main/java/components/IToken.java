package components;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
