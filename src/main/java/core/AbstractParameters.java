package core;

import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public abstract class AbstractParameters implements ITunableParameters {

    List<String> parameterNames = new ArrayList<>();
    Map<String, List<?>> possibleValues = new HashMap<>();
    Map<String, Object> defaultValues = new HashMap<>();
    Map<String, Object> currentValues = new HashMap<>();
    Map<String, Class<?>> parameterTypes = new HashMap<>();

    public AbstractParameters(long seed) {
        addTunableParameter("seed", seed);
    }

    public <T> void addTunableParameter(String name, T defaultValue) {
        parameterNames.add(name);
        defaultValues.put(name, defaultValue);
        parameterTypes.put(name, defaultValue.getClass());
        possibleValues.put(name, Collections.singletonList(defaultValue));
        currentValues.put(name, defaultValue);
    }

    /**
     * Returns a list of IDs for all parameters
     *
     * @return list of parameter IDs
     */
    @Override
    public List<Integer> getParameterIds() {
        return IntStream.range(0, parameterNames.size()).boxed().collect(toList());
    }

    /**
     * Retrieves the default value for the given parameter (as per original game).
     *
     * @param parameterId - ID of parameter queried
     * @return default value for this parameter
     */
    @Override
    public Object getDefaultParameterValue(String parameterName) {
        return defaultValues.get(parameterName);
    }

    /**
     * Sets the value of the given parameter.
     *
     * @param parameterId - ID of parameter to set
     * @param value       - new value for parameter
     */
    @Override
    public void setParameterValue(int parameterId, Object value) {
        currentValues.put(parameterId, value);
    }

    /**
     * Retrieve the values of one parameter.
     *
     * @param parameterId - ID of parameter queried
     */
    @Override
    public Object getParameterValue(int parameterId) {
        return currentValues.get(parameterId);
    }

    /**
     * Retrieve the name of one parameter.
     *
     * @param parameterId - ID of parameter queried
     */
    @Override
    public String getParameterName(int parameterId) {
        return parameterNames.get(parameterId);
    }

    @Override
    public String getJSONDescription() {
        // TODO: To be implemented
        return null;
    }
    /* Methods to be implemented by subclass */

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     * @return - new game parameters object.
     */
    protected abstract AbstractParameters _copy();

    /**
     * Checks if the given object is the same as the current.
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    protected abstract boolean _equals(Object o);


    /* Public API */

    /**
     * Retrieve the random seed for this game.
     * @return - random seed.
     */
    public long getRandomSeed() {
        return get;
    }

    /**
     * Copy this game parameter object.
     * @return - new object with the same parameters, but a new random seed.
     */
    public AbstractParameters copy() {
        AbstractParameters copy = _copy();

        return copy;
    }

    /**
     * Randomizes the set of parameters, if this is a class that implements the TunableParameters interface.
     */
    public void randomize() {
        if (this instanceof ITunableParameters) {
            Random rnd = new Random(randomSeed);
            Map<Integer, ArrayList<?>> searchSpace = ((ITunableParameters)this).getSearchSpace();
            for (Map.Entry<Integer, ArrayList<?>> parameter: searchSpace.entrySet()) {
                int nValues = parameter.getValue().size();
                int randomChoice = rnd.nextInt(nValues);
                ((ITunableParameters)this).setParameterValue(parameter.getKey(), parameter.getValue().get(randomChoice));
            }
        } else {
            System.out.println("Error: Not implementing the TunableParameters interface. Not randomizing");
        }
    }

    /**
     * Resets the set of parameters to their default values, if this is a class that implements the TunableParameters
     * interface.
     */
    public void reset() {
        Map<Integer, Object> defaultValues = getDefaultParameterValues();
        setParameterValues(defaultValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractParameters)) return false;
        AbstractParameters that = (AbstractParameters) o;
        return randomSeed == that.randomSeed && _equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomSeed);
    }


    /**
     * Instantiate parameters from a JSON file
     *
     * @param filename The file with the JSON format data
     * @return The full set of parameters extracted from the file
     */
    public void loadFromJSONFile(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            loadFromJSON(rawData);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage() + " : problem loading TunableParameters from file " + filename);
        }
    }

    public void loadFromJSON(JSONObject rawData) {
        Collection<String> expectedKeys = this.getParameterNames().values();

        for (int id : getParameterIds()) {
            String name = getParameterName(id);
            Object jsonContent = getParam(name, rawData, getParameterValue(id));
            if (jsonContent instanceof List) {
                // then we have a set of values to use

            } else {
                setParameterValue(id, jsonContent);
            }
        }

        // We should also check that there are no other properties in the JSON file
        for (Object key : rawData.keySet()) {
            if (key instanceof String && !expectedKeys.contains(key)) {
                System.out.println("Unexpected key in JSON for TunableParameters : " + key);
            }
        }

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
