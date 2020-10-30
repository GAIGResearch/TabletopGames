package core.interfaces;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.mcts.MCTSParams;

import java.io.FileReader;
import java.util.*;

public interface ITunableParameters {

    /**
     * Retrieve a mapping from int ID to list of possible values, one entry for each parameter.
     * @return - HashMap representing search space (i.e. all possible value configurations)
     */
    Map<Integer, ArrayList<?>> getSearchSpace();

    /**
     * Returns a list of IDs for all parameters
     * @return list of parameter IDs
     */
    List<Integer> getParameterIds();

    /**
     * Retrieves the default values of all parameters (as per original game).
     * @return mapping from int ID of parameter to its default value.
     */
    default Map<Integer, Object> getDefaultParameterValues() {
        List<Integer> parameterIds = getParameterIds();
        Map<Integer, Object> defaultValues = new HashMap<>();
        for (int parameterId: parameterIds) {
            defaultValues.put(parameterId, getParameterValue(parameterId));
        }
        return defaultValues;
    }

    /**
     * Retrieves the default value for the given parameter (as per original game).
     * @param parameterId - ID of parameter queried
     * @return default value for this parameter
     */
    Object getDefaultParameterValue(int parameterId);

    /**
     * Set the values of the parameters, according to the map passed to this method. Each entry maps the int ID of
     * a parameter to its assigned value.
     * @param values - mapping from int ID of parameter to its new value.
     */
    default void setParameterValues(Map<Integer, Object> values) {
        List<Integer> parameterIds = getParameterIds();
        for (int parameterId: parameterIds) {
            setParameterValue(parameterId, values.get(parameterId));
        }
    }

    /**
     * Sets the value of the given parameter.
     * @param parameterId - ID of parameter to set
     * @param value - new value for parameter
     */
    void setParameterValue(int parameterId, Object value);

    /**
     * Retrieve the values of all parameters.
     * @return mapping from int ID of parameter to its current value.
     */
    default Map<Integer, Object> getParameterValues() {
        List<Integer> parameterIds = getParameterIds();
        Map<Integer, Object> currentValues = new HashMap<>();
        for (int parameterId: parameterIds) {
            currentValues.put(parameterId, getParameterValue(parameterId));
        }
        return currentValues;
    }

    /**
     * Retrieve the values of one parameter.
     * @param parameterId - ID of parameter queried
     */
    Object getParameterValue(int parameterId);

    /**
     * Names all parameters for printing purposes.
     * @return mapping from int ID of parameter to parameter name.
     */
    default Map<Integer, String> getParameterNames() {
        List<Integer> parameterIds = getParameterIds();
        Map<Integer, String> names = new HashMap<>();
        for (int parameterId: parameterIds) {
            names.put(parameterId, getParameterName(parameterId));
        }
        return names;
    }

    /**
     * Retrieve the name of one parameter.
     * @param parameterId - ID of parameter queried
     */
    String getParameterName(int parameterId);

    /**
     * @param settings The precise settings to be used. The keys
     * @return Returns an instance of the Generic T created using the specified settings
     */
    default Object instantiate(Map<Integer, Object> settings) {
        setParameterValues(settings);
        return instantiate();
    }

    /**
     * @return Returns the Generic T corresponding to the current settings
     *         (will use all defaults if setParameterValue has not been called at all)
     */
    Object instantiate();

    String getJSONDescription();

    /**
     * Instantiate parameters from a JSON file
     *
     * @param filename The file with the JSON format data
     * @return The full set of parameters extracted from the file
     */
    public static ITunableParameters fromJSONFile(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            return fromJSON(rawData);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage() + " : problem loading TunableParameters from file " + filename);
        }
    }

    static ITunableParameters fromJSON(JSONObject rawData) {
        long seed = getParam("seed", rawData, System.currentTimeMillis());
        MCTSParams retValue = new MCTSParams(seed);

        retValue.K = getParam("K", rawData, retValue.K);
        retValue.rolloutLength = getParam("rolloutLength", rawData, retValue.rolloutLength);
        retValue.rolloutsEnabled = getParam("rolloutsEnabled", rawData, retValue.rolloutsEnabled);
        retValue.epsilon = getParam("epsilon", rawData, retValue.epsilon);
        retValue.rolloutType = getParam("rolloutType", rawData, retValue.rolloutType);
        retValue.budgetType = getParam("budgetType", rawData, retValue.budgetType);
        int budget = getParam("budget", rawData, -1);
        switch (retValue.budgetType) {
            case PlayerConstants.BUDGET_TIME:
                retValue.timeBudget = (budget == -1) ? retValue.timeBudget : budget;
                break;
            case PlayerConstants.BUDGET_ITERATIONS:
                retValue.iterationsBudget = (budget == -1) ? retValue.iterationsBudget : budget;
                break;
            case PlayerConstants.BUDGET_FM_CALLS:
                retValue.fmCallsBudget = (budget == -1) ? retValue.fmCallsBudget : budget;
                break;
            default:
                throw new AssertionError("Unknown Budget Type " + retValue.budgetType);
        }

        // We should also check that there are no other properties in
        for (Object key : rawData.keySet()) {
            if (key instanceof String && !expectedKeys.contains(key)) {
                System.out.println("Unexpected key in JSON for MCTSParameters : " + key);
            }
        }

        return retValue;
    }

    /**
     * @param name Name of the parameter. This will be validated as one of a possible set of expectedKeys
     * @param json The JSONObject containing the data we want to extract the parameter from.
     * @param defaultValue The default value to use for the parameter if we can't find it in json.
     * @param <T> The class of the parameter (anticipated as one of Integer, Double, String, Boolean)
     * @return The value of the parameter found.
     */
    @SuppressWarnings("unchecked")
    static <T> T getParam(String name, JSONObject json, T defaultValue) {
        Object data = json.getOrDefault(name, defaultValue);
        if (data.getClass() == defaultValue.getClass())
            return (T) data;
        return defaultValue;
    }

}
