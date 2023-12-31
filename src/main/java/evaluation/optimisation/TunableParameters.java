package evaluation.optimisation;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.JSONUtils;

import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * A sub-class of AbstractParameters that implements the ITunableParameters interface
 * <p>
 * The idea is that any tunable parameters can inherit from this. Any sub-class can make a parameter visible
 * via the ITunableParameters interface by calling either :
 * addTunableParameter(name, defaultValue), or
 * addTunableParameter(name, defaultValue, allPossibleValues)
 * <p>
 * Using the first of these means the parameter will not be tunable, but it will be included in all json representations
 * of the Parameters. This can be useful to set up defaults outside of code.
 * <p>
 * Any inheriting class does not need to explicitly copy any data inserted via addTunableParameter(...)
 */
public abstract class TunableParameters extends AbstractParameters implements ITunableParameters {

    private static boolean debug = false;
    List<String> parameterNames = new ArrayList<>();
    Map<String, List<Object>> possibleValues = new HashMap<>();
    Map<String, Object> defaultValues = new HashMap<>();
    Map<String, Object> currentValues = new HashMap<>();
    Map<String, Class<?>> parameterTypes = new HashMap<>();

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
            e.printStackTrace();
            throw new AssertionError(e.getClass().toString() + " : " + e.getMessage() + " : problem loading TunableParameters from file " + filename);
        }
    }

    /**
     * Instantiate parameters from a JSONObject
     */
    public static void loadFromJSON(TunableParameters params, JSONObject rawData) {
        List<String> allParams = params.getParameterNames();
        for (String pName : allParams) {
            if (debug)
                System.out.println("\tLoading " + pName);
            if (isParamArray(pName, rawData)) {
                List<?> pValue = getParamList(pName, rawData, params.getDefaultParameterValue(pName));
                params.addTunableParameter(pName, params.getDefaultParameterValue(pName),
                        new ArrayList<>(pValue));
            } else {
                Object pValue = getParam(pName, rawData, params.getDefaultParameterValue(pName), params);
                if (pValue != null)
                    params.addTunableParameter(pName, pValue);
            }
        }
        params._reset();

        // We should also check that there are no other properties in there
        allParams.add("class");
        for (Object key : rawData.keySet()) {
            if (key instanceof String && !allParams.contains(key)) {
                System.out.println("Unexpected key in JSON for TunableParameters : " + key);
            }
        }
    }

    /**
     * @param name         Name of the parameter. This will be validated as one of a possible set of expectedKeys
     * @param json         The JSONObject containing the data we want to extract the parameter from.
     * @param defaultValue The default value to use for the parameter if we can't find it in json.
     * @param <T>          The class of the parameter (anticipated as one of Integer, Double, String, Boolean)
     * @return The value of the parameter found.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getParam(String name, JSONObject json, T defaultValue, TunableParameters params) {
        Object finalData = json.getOrDefault(name, defaultValue);
        if (finalData == null)
            return null;
        Object data = (finalData instanceof Long) ? Integer.valueOf(((Long) finalData).intValue()) : finalData;
        if (finalData instanceof JSONObject) {
            JSONObject subJson = (JSONObject) finalData;
            T retValue = JSONUtils.loadClassFromJSON(subJson);
            if (retValue instanceof TunableParameters) {
                TunableParameters subParams = (TunableParameters) retValue;
                TunableParameters.loadFromJSON(subParams, subJson);
                params.setParameterValue(name, subParams);
            //    params.registerChild(name, subJson);
            }
            return retValue;
        }
        if (defaultValue == null) {
            throw new AssertionError("No default value provided for " + name + " to check JSON validity");
        }
        if (data.getClass() == defaultValue.getClass())
            return (T) data;
        if (data.getClass() == Integer.class && defaultValue.getClass() == Double.class)
            return (T) Double.valueOf((Integer) data);
        if (data.getClass() == String.class && defaultValue.getClass().isEnum()) {
            Optional<?> matchingValue = Arrays.stream(defaultValue.getClass().getEnumConstants()).filter(e -> e.toString().equals(data)).findFirst();
            if (matchingValue.isPresent()) {
                return (T) matchingValue.get();
            }
            throw new AssertionError("No Enum match found for " + name + " [" + data + "] in " + Arrays.toString(defaultValue.getClass().getEnumConstants()));
        }
        System.out.println("Warning: parsing param " + name + "; couldn't find correct type, assigning default value: " + defaultValue);
        return defaultValue;
    }

    private static boolean isParamArray(String name, JSONObject json) {
        return (json.get(name) instanceof List);
    }

    private static boolean isParamJSON(String name, JSONObject json) {
        return (json.get(name) instanceof Map);
    }

    private static <T> List<T> getParamList(String name, JSONObject json, T defaultValue) {
        Object data = json.getOrDefault(name, defaultValue);
        if (!(data instanceof List))
            throw new AssertionError("JSON does not contain an Array as expected for " + name);
        return (List<T>) data;
    }

    /**
     * Note that any sub-class of TunableParameters does not need to do any copying of parameters added
     * via addTunableParameter(). These are copied here.
     * <p>
     * Sub-classes should NOT need to implement copy(). Instead they should just implement _copy(), and return
     * an empty copy of themselves, with any non-tunable parameters set appropriately.
     *
     * @return The copied Parameters
     */
    @Override
    public TunableParameters copy() {
        AbstractParameters retValue = super.copy();  // this calls ._copy()
        TunableParameters tunable = (TunableParameters) retValue;
        tunable.parameterNames = new ArrayList<>(parameterNames);
        tunable.possibleValues = new HashMap<>(possibleValues);
        tunable.defaultValues = new HashMap<>(defaultValues);
        tunable.parameterTypes = new HashMap<>(parameterTypes);
        for (String name : parameterNames) {
            Object value = getParameterValue(name);
            if (value instanceof TunableParameters) {
                // then we have to recurse
                TunableParameters subParams = (TunableParameters) value;
                TunableParameters subParamsCopy = subParams.copy();
                tunable.setParameterValue(name, subParamsCopy);
            } else {
                tunable.setParameterValue(name, value);
            }
        }
        tunable._reset();
        return tunable;
    }

    /**
     * Use this to add a non-Tunable Parameter (i.e. one with a single value that does not change)
     * While this is not tuned, it means that a value for it can be defined in a JSON input file
     * <p>
     * A tunable parameter can be any of Integer, Double, String, Enum, Boolean, List
     *
     * @param name  The name of the parameter
     * @param value The value this should take
     * @param <T>   The type of the parameter
     */
    public <T> void addTunableParameter(String name, T value) {
        addTunableParameter(name, value, Collections.singletonList(value));
    }

    /**
     * Use this to add a Tunable Parameter. Parameters added using this method when instantiating a Params object
     * will be the default search dimensions used in optimisation.
     * Note however, that these values are not exclusive.
     * ParameterSearch permits a JSON file to be specified that provides a way of overriding (adding to and/or
     * subtracting from) the values to be used. (See ParameterSearch documentation.)
     *
     * @param name         The name of the parameter
     * @param defaultValue The default value this should take
     * @param allSettings  All possible (non-exhaustive, just a reasonable set) values this can take.
     * @param <T>          The type of the parameter
     */
    public <T> void addTunableParameter(String name, T defaultValue, List<T> allSettings) {
        if (!parameterNames.contains(name)) parameterNames.add(name);
        defaultValues.put(name, defaultValue);
        parameterTypes.put(name, defaultValue.getClass());
        possibleValues.put(name, new ArrayList<>(allSettings));
        currentValues.put(name, defaultValue);
    }

    public <T> void addTunableParameter(String name, Class<T> classType) {
        if (!parameterNames.contains(name)) parameterNames.add(name);
        defaultValues.put(name, null);
        parameterTypes.put(name, classType);
        possibleValues.put(name, new ArrayList<>());
        currentValues.put(name, null);
    }

    /**
     * Names all parameters for printing purposes.
     *
     * @return mapping from int ID of parameter to parameter name.
     */
    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
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
        if (parameterName.split(Pattern.quote(".")).length > 1) {
            // in this case we pass on to the subParam (as well as updating here)
            String[] split = parameterName.split(Pattern.quote("."));
            String subParamName = split[0];
            String subParam = parameterName.substring(subParamName.length() + 1);
            ((ITunableParameters)getParameterValue(subParamName)).setParameterValue(subParam, value);
        }
        if (parameterTypes.get(parameterName).isEnum() && value instanceof String) {
            Object[] values = parameterTypes.get(parameterName).getEnumConstants();
            Optional<Object> found = Arrays.stream(values).filter(v -> v.toString().equals(value)).findFirst();
            if (found.isPresent())
                currentValues.put(parameterName, found.get());
            else
                throw new AssertionError("No corresponding Enum found for " + value + " in " + parameterName);
        } else {
            Object finalValue = value;
            if (value instanceof Long)
                finalValue = ((Long) value).intValue();
            currentValues.put(parameterName, finalValue);
        }
        // Then, if value is TunableParameter itself, we 'lift' its attributes up to the top level
        // and remove any previous ones
        if (value instanceof TunableParameters) {
            TunableParameters subParams = (TunableParameters) value;
            List<String> oldParamNames = parameterNames.stream().filter(n -> n.startsWith(parameterName + ".")).collect(toList());
            // we now remove these
            oldParamNames.forEach(parameterNames::remove);
            oldParamNames.forEach(possibleValues::remove);
            oldParamNames.forEach(defaultValues::remove);
            oldParamNames.forEach(currentValues::remove);
            for (String name : subParams.getParameterNames()) {
                String liftedName = parameterName + "." + name;
                addTunableParameter(liftedName, subParams.getDefaultParameterValue(name), subParams.getPossibleValues(name));
                setParameterValue(liftedName, subParams.getParameterValue(name));
            }
        }

        // this sets the value within ITunableParameters; but this may need to be transposed to the local field
        _reset();
    }

    /**
     * Method that reloads all the locally stored values from currentValues
     * This is in case sub-classes decide to use the frankly more intuitive access via
     * params.paramName
     * instead of
     * params.getParameterValue("paramName")
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

    @Override
    public String getJSONDescription() {
        JSONObject retValue = new JSONObject();
        retValue.put("parametersType", this.getClass().getName());
        parameterNames.forEach(name -> {
            Class<?> clazz = parameterTypes.get(name);
            if (possibleValues.getOrDefault(name, Collections.emptyList()).size() > 1) {
                List<Object> values = possibleValues.get(name);
                if (clazz.isEnum())
                    values = values.stream().map(Object::toString).collect(toList());
                // TODO: Possibly something here to allow for JSONObjects to be passed in as well
                retValue.put(name, values);
            } else {
                // we have a single value, or a missing
                Object value = defaultValues.getOrDefault(name, null);
                if (clazz.isEnum()) value = value.toString();
                retValue.put(name, value);
            }
        });
        return retValue.toJSONString();
    }

    /**
     * Retrieve the values of all parameters.
     *
     * @return mapping from int ID of parameter to its current value.
     */
    @Override
    public Map<String, Object> getParameterValues() {
        return new HashMap<>(currentValues);
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
        for (Object key : values.keySet()) {
            if (key instanceof String) {
                setParameterValue((String) key, values.get(key));
            } else {
                String name = getParameterName((int) key);
                setParameterValue(name, values.get(key));
            }
        }
    }

    /**
     * Names all parameters for printing purposes.
     *
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
        // getRandomSeed() == that.getRandomSeed() && removed, so that equals (and hashcode) covers parameters only
        return  _equals(o)
                && that.parameterNames.equals(parameterNames)
                && that.possibleValues.equals(possibleValues)
                && that.currentValues.equals(currentValues)
                && that.defaultValues.equals(defaultValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameterNames, possibleValues, defaultValues, currentValues);
    }
}
