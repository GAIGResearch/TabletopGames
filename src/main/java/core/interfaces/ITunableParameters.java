package core.interfaces;

import java.util.*;

/**
 * @param <T> This should either be AbstractPlayer or Game (or a subclass thereof),
 *            as these are the two items that are 'Tunable'.
 *            (although this list may expand in the future)
 */
public interface ITunableParameters<T> {

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
    default T instantiate(Map<Integer, Object> settings) {
        setParameterValues(settings);
        return instantiate();
    }

    /**
     * @return Returns the Generic T corresponding to the current settings
     *         (will use all defaults if setParameterValue has not been called at all)
     */
    T instantiate();

}
