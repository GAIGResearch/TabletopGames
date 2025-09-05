package core.interfaces;

import org.json.simple.JSONObject;

public interface IToJSON {

    /**
     * Converts this object to a JSON string.
     * @return - JSON string
     */
    JSONObject toJSON();
}
