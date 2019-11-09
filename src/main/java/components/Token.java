package components;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import content.PropertyString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;
import utilities.Utils.ComponentType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class Token extends Component {
    // position should be reference to a graph
    private int position;           // todo graph
    private HashSet<Integer> owner; // owner

    private String token_type;      // string type
    private String name;            // name (id)
    private int occurenceLimit;     // occurence limit

    public Token(){
        super.type = ComponentType.TOKEN;
        properties = new HashMap<>();
    }

    public Token copy(){
        Token copy = new Token();
        copy.position = position;
        copy.token_type = new String(token_type);
        copy.owner = (HashSet)owner.clone();
        copy.occurenceLimit = occurenceLimit;

        //copy type and component.
        copyComponentTo(copy);

        return copy;
    }

    public void loadToken(JSONObject token) {

        this.name = (String) ( (JSONArray) token.get("name")).get(1);
        this.token_type = (String) ( (JSONArray) token.get("type")).get(1);
        this.occurenceLimit = ((Long) ( (JSONArray) token.get("count")).get(1)).intValue();

        parseComponent(this, token);
    }


    public static List<Component> loadTokens(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Component> tokens = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Token newToken = new Token();
                newToken.loadToken((JSONObject) o);
                tokens.add(newToken);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return tokens;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public HashSet<Integer> getOwner() {
        return owner;
    }

    public void setOwner(HashSet<Integer> owner) {
        this.owner = owner;
    }

    public int getOccurenceLimit() {
        return occurenceLimit;
    }

    public void setOccurenceLimit(int occurenceLimit) {
        this.occurenceLimit = occurenceLimit;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {

        return position;
    }
}
