package core.interfaces;

import org.json.simple.JSONObject;

import java.util.*;


public interface ITunableParameters<T> {

    /**
     * Returns a list of IDs for all parameters
     *
     * @return list of parameter IDs
     */
    List<Integer> getParameterIds();

    /**
     * Retrieve the name of one parameter.
     *
     * @param parameterId - ID of parameter queried
     */
    String getParameterName(int parameterId);

    /**
     * Retrieve the values of one parameter.
     *
     * @param name - name of parameter queried
     */
    Object getParameterValue(String name);

    /**
     * Retrieves the default value for the given parameter (as per original game).
     *
     * @param name - ID of parameter queried
     * @return default value for this parameter
     */
    Object getDefaultParameterValue(String name);

    /**
     * Sets the value of the given parameter.
     *
     * @param name  - ID of parameter to set
     * @param value - new value for parameter
     */
    void setParameterValue(String name, Object value);

    /**
     * Provides a list of all the possible settings that this parameter could be set to
     *
     * @param name The Id of the parameter that we want the possible value for
     * @return A List of all the possible values this can take
     */
    List<Object> getPossibleValues(String name);

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    T instantiate();

    /**
     * @return a TunableParameters object instantiated from JSON
     * This only supports specific settings, and not the full range of possible settings
     * (For that use a SearchSpace)
     * @param jsonObject
     */
    ITunableParameters<T> instanceFromJSON(JSONObject jsonObject);

    /**
     *
     * @return a JSON representation of the current parameter settings
     * This is designed to be used for saving the current settings to a file for later instantiation
     * with fromJSON
     */
    JSONObject instanceToJSON(boolean excludeDefaultValues, Map<String, Integer> settings);

    /**
     * Retrieves the default values of all parameters (as per original game).
     *
     * @return mapping from int ID of parameter to its default value.
     */
    default Map<String, Object> getDefaultParameterValues() {
        Map<String, Object> defaultValues = new HashMap<>();
        for (String name : getParameterNames()) {
            defaultValues.put(name, getParameterValue(name));
        }
        return defaultValues;
    }

    /**
     * Set the values of the parameters, according to the map passed to this method. Each entry maps the int ID of
     * a parameter to its assigned value.
     *
     * @param values - mapping from int ID of parameter to its new value.
     *               or mapping from the name of the parameter as a String
     */
    default void setParameterValues(Map<?, Object> values) {
        for (Object descriptor : values.keySet()) {
            String name = (descriptor instanceof String) ? (String) descriptor : getParameterName((Integer) descriptor);
            setParameterValue(name, values.get(descriptor));
        }
    }

    /**
     * Method that reloads all the locally stored values from currentValues
     * This is in case sub-classes decide to use the frankly more intuitive access via
     * params.paramName
     * instead of
     * params.getParameterValue("paramName")
     * (the latter is also more typo-prone if we hardcode strings everywhere)
     */
     void _reset();


    /**
     * Retrieve the values of all parameters.
     *
     * @return mapping from int ID of parameter to its current value.
     */
    default Map<String, Object> getParameterValues() {
        Map<String, Object> currentValues = new HashMap<>();
        for (String name : getParameterNames()) {
            currentValues.put(name, getParameterValue(name));
        }
        return currentValues;
    }

    /**
     * Names all parameters for printing purposes.
     *
     * @return mapping from int ID of parameter to parameter name.
     */
    default List<String> getParameterNames() {
        List<String> names = new ArrayList<>();
        for (int parameterId : getParameterIds()) {
            names.add(getParameterName(parameterId));
        }
        return names;
    }

    /**
     * Names all parameters for printing purposes.
     *
     * @return mapping from int ID of parameter to parameter name.
     */
    Map<String, Class<?>> getParameterTypes();

}
