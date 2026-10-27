package core.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import core.CoreConstants;
import core.interfaces.IToJSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.List;

public class Token extends Component implements IToJSON {
    protected String tokenType;

    public Token(String name){
        super(CoreConstants.ComponentType.TOKEN, name);
        this.tokenType = name;
    }

    public Token(String name, int ID){
        super(CoreConstants.ComponentType.TOKEN, name, ID);
        this.tokenType = name;
    }

    @Override
    public Token copy(){
        Token copy = new Token(componentName, componentID);
        copy.tokenType = tokenType;
        copyComponentTo(copy);
        return copy;
    }

    /**
     * @return - the type of this token.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the type of this token.
     * @param tokenType - new type for this token.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Loads all tokens from a JSON file.
     * @param filename - path to file.
     * @return - List of Token objects.
     */
    public static List<Token> loadTokens(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Token> tokens = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Token newToken = new Token("");
                newToken.loadToken((JSONObject) o);
                tokens.add(newToken);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return tokens;
    }

    /**
     * Creates a Token objects from a JSON object.
     * @param token - JSON to parse into Token object.
     */
    protected void loadToken(JSONObject token) {
        if (token.containsKey("tokenType")) {
            this.tokenType = (String) token.get("tokenType");
        }

        if (token.containsKey("name")) {
            this.componentName = (String) token.get("name");
        }

        if (token.containsKey("ownerId")) {
            this.ownerId = ((Number) token.get("ownerId")).intValue();
        }

        parseComponent(this, token);
    }

    @Override
    public int hashCode() {
        return componentID;
    }

    @Override
    public String toString() {
        return tokenType;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject tJSON = new JSONObject();
        tJSON.put("name", componentName);
        tJSON.put("ownerId", ownerId);
        tJSON.put("id", componentID);
        tJSON.put("tokenType", tokenType);
        return tJSON;
    }

    public static Token loadFromJSON(JSONObject tJSON) {
        Token t;
        if (tJSON.containsKey("id") && tJSON.get("id") instanceof Number) {
            t = new Token("", ((Number) tJSON.get("id")).intValue());
        } else {
            t = new Token("");
        }
        t.loadToken(tJSON);
        return t;
    }
}
