package core.interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface ITunableParameters {

    /**
     * Retrieve a mapping from int ID to list of possible values, one entry for each parameter.
     * @return - HashMap representing search space (i.e. all possible value configurations)
     */
    HashMap<Integer, ArrayList<?>> getSearchSpace();

    /**
     * Returns a list of IDs for all parameters
     * @return list of parameter IDs
     */
    List<Integer> getParameterIds();

    /**
     * Retrieves the default values of all parameters (as per original game).
     * @return mapping from int ID of parameter to its default value.
     */
    default HashMap<Integer, Object> getDefaultParameterValues() {
        List<Integer> parameterIds = getParameterIds();
        HashMap<Integer, Object> defaultValues = new HashMap<>();
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
    default void setParameterValues(HashMap<Integer, Object> values) {
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
    default HashMap<Integer, Object> getParameterValues() {
        List<Integer> parameterIds = getParameterIds();
        HashMap<Integer, Object> currentValues = new HashMap<>();
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
    default HashMap<Integer, String> getParameterNames() {
        List<Integer> parameterIds = getParameterIds();
        HashMap<Integer, String> names = new HashMap<>();
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

}
