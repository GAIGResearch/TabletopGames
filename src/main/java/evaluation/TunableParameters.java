package evaluation;

import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.mcts.MCTSParams;
import utilities.Hash;

import java.io.FileReader;
import java.util.*;

import static java.util.stream.Collectors.*;

public abstract class TunableParameters implements ITunableParameters {

    Map<Integer, String> parameterNames = new HashMap<>();
    Map<Integer, ArrayList<?>> possibleValues = new HashMap<>();
    Map<Integer, Object> currentValues = new HashMap<>();
    Map<Integer, Class<?>> parameterTypes = new HashMap<>();

    /**
     * Returns a list of IDs for all parameters
     *
     * @return list of parameter IDs
     */
    @Override
    public List<Integer> getParameterIds() {
        return null;
    }

    /**
     * Retrieves the default value for the given parameter (as per original game).
     *
     * @param parameterId - ID of parameter queried
     * @return default value for this parameter
     */
    @Override
    public Object getDefaultParameterValue(int parameterId) {
        return null;
    }

    /**
     * Sets the value of the given parameter.
     *
     * @param parameterId - ID of parameter to set
     * @param value       - new value for parameter
     */
    @Override
    public void setParameterValue(int parameterId, Object value) {

    }

    /**
     * Retrieve the values of one parameter.
     *
     * @param parameterId - ID of parameter queried
     */
    @Override
    public Object getParameterValue(int parameterId) {
        return null;
    }

    /**
     * Retrieve the name of one parameter.
     *
     * @param parameterId - ID of parameter queried
     */
    @Override
    public String getParameterName(int parameterId) {
        return null;
    }

    /**
     * @return Returns the Generic T corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public Object instantiate() {
        return null;
    }

    @Override
    public String getJSONDescription() {
        return null;
    }


}
