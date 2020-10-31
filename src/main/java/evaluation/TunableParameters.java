package evaluation;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.mcts.MCTSParams;
import utilities.Hash;

import java.io.FileReader;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

/**
 * A sub-class of AbstractParameters that implements the ITunableParameters interface
 *
 * The idea is that any tunable parameters can inherit from this. Any sub-class can make a parameter visible
 * via the ITunableParameters interface by calling either :
 *      addTunableParameter(name, defaultValue), or
 *      addTunableParameter(name, defaultValue, allPossibleValues)
 *
 * Using the first of these means the parameter will not be tunable, but it will be included in all json representations
 * of the Parameters. This can be useful to set up defaults outside of code.
 *
 * Any inheriting class does not need to explicitly copy any data inserted via addTunableParameter(...)
 */
public abstract class TunableParameters extends AbstractParameters implements ITunableParameters {

    /**
     * Zero-argument constructor used for auto-tuning
     */
    public TunableParameters() {
        this(System.currentTimeMillis());
    }
    public TunableParameters(long seed) {
        super(seed);
    }

    List<String> parameterNames = new ArrayList<>();
    Map<String, List<Object>> possibleValues = new HashMap<>();
    Map<String, Object> defaultValues = new HashMap<>();
    Map<String, Object> currentValues = new HashMap<>();
    Map<String, Class<?>> parameterTypes = new HashMap<>();

    @Override
    public AbstractParameters copy() {
        AbstractParameters retValue = super.copy();
        TunableParameters tunable = (TunableParameters) retValue;
        tunable.parameterNames = new ArrayList<>(parameterNames);
        tunable.possibleValues = new HashMap<>(possibleValues);
        tunable.defaultValues = new HashMap<>(defaultValues);
        tunable.parameterTypes = new HashMap<>(parameterTypes);
        return tunable;
    }

    /**
     * Use this to add a non-Tunable Parameter (i.e. one with a single value that does not change)
     * While this is not tuned, it means that a value for it can be defined in a JSON input file
     *
     * @param name          The name of the parameter
     * @param value         The value this should take
     * @param <T>           The type of the parameter
     */
    public <T> void addTunableParameter(String name, T value) {
        addTunableParameter(name, value, Collections.singletonList(value));
    }
    public <T> void addTunableParameter(String name, T defaultValue, List<T> allSettings) {
        parameterNames.add(name);
        defaultValues.put(name, defaultValue);
        parameterTypes.put(name, defaultValue.getClass());
        possibleValues.put(name, new ArrayList<>(allSettings));
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
     * @param parameterName - ID of parameter queried
     * @return default value for this parameter
     */
    @Override
    public Object getDefaultParameterValue(String parameterName) {
        return defaultValues.get(parameterName);
    }

    /**
     * Sets the value of the given parameter.
     *
     * @param parameterName - ID of parameter to set
     * @param value         - new value for parameter
     */
    @Override
    public void setParameterValue(String parameterName, Object value) {
        currentValues.put(parameterName, value);
        _reset();
    }

    /**
     * Method that reloads all the locally stored values from currentValues
     * This is in case sub-classes decide to use the frankly more intuitive access via
     *   params.paramName
     * instead of
     *   params.getParameterValue("paramName")
     * (the latter is also more typo-prone if we hardcode strings everywhere)
     */
    abstract public void _reset();

    /**
     * Retrieve the values of one parameter.
     *
     * @param parameterName - ID of parameter queried
     */
    @Override
    public Object getParameterValue(String parameterName) {
        return currentValues.get(parameterName);
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

    /**
     * Provides a list of all the possible settings that this parameter could be set to
     *
     * @param name The Id of the parameter that we want the possible value for
     * @return A List of all the possible values this can take
     */
    @Override
    public List<Object> getPossibleValues(String name) {
        return new ArrayList<>(possibleValues.getOrDefault(name, Collections.emptyList()));
    }

    /**
     * Retrieves the default values of all parameters (as per original game).
     *
     * @return mapping from int ID of parameter to its default value.
     */
    @Override
    public Map<String, Object> getDefaultParameterValues() {
        return new HashMap<>(defaultValues);
    }

    /**
     * Set the values of the parameters, according to the map passed to this method. Each entry maps the int ID of
     * a parameter to its assigned value.
     *
     * @param values - mapping from int ID of parameter to its new value.
     *               or mapping from the name of the parameter as a String
     */
    @Override
    public void setParameterValues(Map<?, Object> values) {
        for ( Object key : values.keySet()) {
            if (key instanceof String) {
                setParameterValue((String) key, values.get(key));
            } else {
                String name = getParameterName((int) key);
                setParameterValue(name, values.get(key));
            }
        }
    }

    @Override
    public String getJSONDescription() {
        JSONObject retValue = new JSONObject();
        retValue.put("parametersType", this.getClass().getName());
        parameterNames.forEach( name -> {
            if (possibleValues.getOrDefault(name, Collections.emptyList()).size() > 1) {
                retValue.put(name, possibleValues.get(name));
            } else {
                // we have a single value, or a missing
                retValue.put(name, defaultValues.getOrDefault(name, null));
            }
        });
        return retValue.toJSONString();
    }

    /**
     * Instantiate parameters from a JSON file
     *
     * @param filename The file with the JSON format data
     */
    public static void loadFromJSONFile(TunableParameters params, String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            loadFromJSON(params, rawData);
        } catch (Exception e) {
            throw new AssertionError(e.getClass().toString() + " : " + e.getMessage() + " : problem loading TunableParameters from file " + filename);
        }
    }

    /**
     * Instantiate paramaters from a JSONObject
     */
    public static void loadFromJSON(TunableParameters params, JSONObject rawData) {
        List<String> allParams = params.getParameterNames();
        for (String pName : allParams) {
     //       Class<?> clazz = params.parameterTypes.get(pName);
            if (isParamSingular(pName, rawData)) {
                Object pValue = getParam(pName, rawData, params.getDefaultParameterValue(pName));
                params.addTunableParameter(pName,pValue);
            } else {
                Object pValue = getParamList(pName, rawData, params.getDefaultParameterValue(pName));
                params.addTunableParameter(pName, params.getDefaultParameterValue(pName),
                        new ArrayList<>(((List<?>) pValue)));
            }

        }
/*        int budget = getParam("budget", rawData, -1);
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
        }*/

        // We should also check that there are no other properties in there
        allParams.add("algorithm");
        allParams.add("parametersType");
        for (Object key : rawData.keySet()) {
            if (key instanceof String && !allParams.contains(key)) {
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
    private static <T> T getParam(String name, JSONObject json, T defaultValue) {
        Object data = json.getOrDefault(name, defaultValue);
        if (data.getClass() == defaultValue.getClass())
            return (T) data;
        return defaultValue;
    }
    private static boolean isParamSingular(String name, JSONObject json) {
        return !(json.get(name) instanceof List);
    }
    private static <T> List<T> getParamList(String name, JSONObject json, T defaultValue) {
        Object data = json.getOrDefault(name, defaultValue);
        if (!(data instanceof List))
            throw new AssertionError("JSON does not contain an Array as expected for " + name);
        return (List<T>) data;
    }

    /**
     * Retrieve the values of all parameters.
     * @return mapping from int ID of parameter to its current value.
     */
    @Override
    public Map<String, Object> getParameterValues() {
        return new HashMap<>(currentValues);
    }

    /**
     * Names all parameters for printing purposes.
     * @return mapping from int ID of parameter to parameter name.
     */
    @Override
    public List<String> getParameterNames() {
        return new ArrayList<>(parameterNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TunableParameters)) return false;
        TunableParameters that = (TunableParameters) o;
        return getRandomSeed() == that.getRandomSeed() && _equals(o)
                && that.parameterNames.equals(parameterNames)
                && that.possibleValues.equals(possibleValues)
                && that.defaultValues.equals(defaultValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameterNames, possibleValues, defaultValues);
    }
}
