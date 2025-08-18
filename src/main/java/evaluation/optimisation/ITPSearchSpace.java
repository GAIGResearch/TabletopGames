package evaluation.optimisation;

import core.interfaces.ITunableParameters;
import org.apache.hadoop.shaded.org.eclipse.jetty.util.ajax.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import evaluation.optimisation.ntbea.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

/**
 * This is a wrapper around ITunableParameters<T> (the TAG standard) to implement the AgentSearchSpace
 * interface used by the NTBEA library.
 */


public class ITPSearchSpace<T> extends AgentSearchSpace<T> {

    public record ParameterSettings(String name, Class<?> clazz, List<Object> values) {
    }

    static boolean debug = false;
    ITunableParameters<T> itp;

    /**
     * Constructor of a SearchSpace to use all the default values defined by an
     * implementation of ITunableParameters
     *
     * @param tunableParameters The ITunableParameters object we want to optimise over.
     */
    public ITPSearchSpace(ITunableParameters<T> tunableParameters) {
        itp = tunableParameters;
        List<String> parameterNames = itp.getParameterNames();
        List<ParameterSettings> settings = new ArrayList<>();
        for (String name : parameterNames) {
            settings.add(new ParameterSettings(name, itp.getParameterTypes().get(name), itp.getPossibleValues(name)));
        }
        super.initialise(settings);
    }

    /**
     * Constructor that also takes in a JSON file to override the defaults of the base ITunableParameters
     * It is possible to override the sets of values to be explored for any of the parameters.
     * It is *not* possible to add new parameters in the JSON file - this will cause a validation error to be thrown.
     * For each parameter not to be searched, then a single value can be provided, or the parameter can be excluded
     * from the file, in which case a default value will be obtained from the base ITunableParameters.
     * <p>
     * The constructor will report any parameters in the file that are not valid for the specified ITunableParameters
     * to help spot typos; but this will not cause the
     * <p>
     * Each parameter defined should have one property in the JSON file, for example:
     * "parameterName" : [0.3, 1.0, 3.0, 10.0, 30.0, 100.0 ]
     * If the property is set to an Array, then this defines the values that will be optimised over.
     * If the property is a single value, then this defines the constant value to use for that parameter while the
     * others are optimised.
     *
     * @param tunableParameters An implementation of the ITunableParameters interface
     * @param jsonFile          The location of a JSON file to override the defaults of the ITunableParameters
     *                          interface.
     */
    public ITPSearchSpace(ITunableParameters<T> tunableParameters, String jsonFile) {
        this(tunableParameters, JSONUtils.loadJSONFile(jsonFile));
    }

    public ITPSearchSpace(ITunableParameters<T> tunableParameters, JSONObject json) {
        itp = tunableParameters;
        List<ParameterSettings> parameterTypes = extractRecursiveParameters("", json, itp);
        super.initialise(parameterTypes);
        if (tunableParameters instanceof TunableParameters tp) {
            tp.setRawJSON(json);
        }
    }

    @SuppressWarnings("unchecked")
/**
 * This returns a List of ParameterSettings objects that define the parameters to be optimised over.
 *
 */
    private static List<ParameterSettings> extractRecursiveParameters(String nameSpace, JSONObject json, ITunableParameters<?> itp) {
        List<ParameterSettings> retValue = new ArrayList<>();
        for (Object baseKey : json.keySet()) {
            if (baseKey instanceof String) {
                if (itp.getParameterNames().contains(baseKey)) {
                    Object data = json.get(baseKey);
                    String key = "".equals(nameSpace) ? (String) baseKey : nameSpace + "." + baseKey;
                    // check to see if this is a json file
                    if (data instanceof String str) {
                        if (str.endsWith(".json"))
                            data = JSONUtils.loadJSONFile(str);
                    }
                    if (data instanceof JSONObject) {
                        // in this case we have nesting, and need to recurse to get all the relevant parameters
                        // we use key as the nameSpace
                        try {
                            JSONObject subJSON = (JSONObject) data;
                            subJSON.get("class");
                            if (debug)
                                System.out.println("Starting recursion on " + key);
                            Object child = JSONUtils.loadClassFromJSON(subJSON);
                            itp.setParameterValue((String) baseKey, child);
                            if (child instanceof ITunableParameters)
                                retValue.addAll(extractRecursiveParameters(key, (JSONObject) data, (ITunableParameters) child));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new AssertionError(e.getMessage() + " problem creating SearchSpace " + data);
                        }
                    } else if (data instanceof JSONArray arr) {
                        // we have a set of options for this parameter
                        // We need to instantiate the JSONArray arr into List<?> values (should be instances of clazz)
                        Class<?> clazz = itp.getParameterTypes().get(baseKey);
                        List<Object> values = arr.stream().map(o -> {
                            if (o instanceof JSONObject localJSON) {
                                try {
                                    return JSONUtils.loadClassFromJSON(localJSON);
                                } catch (Exception e) {
                                    throw new AssertionError(e.getMessage() + " problem instantiating from  " + localJSON);
                                }
                            } else {
                                return o;
                            }
                        }).toList();
                        if (debug)
                            System.out.println(baseKey + " : adding " + arr);
                        if (clazz.isEnum()) {
                            Class<? extends Enum> enumCl = (Class<? extends Enum>) clazz;
                            values = arr.stream().map(o -> Enum.valueOf(enumCl, (String) o)).toList();
                        }
                        retValue.add(new ParameterSettings(key, clazz, values));
                    } else {
                        // this defines a default we should be using in itp
                        if (data == null)
                            throw new AssertionError("We have a problem with null data in JSON file using key " + key);
                        String[] namespaceSplit = key.split("\\.");
                        itp.setParameterValue(namespaceSplit[namespaceSplit.length - 1], data);
                        if (debug)
                            System.out.println("Setting default: " + namespaceSplit[namespaceSplit.length - 1] + " = " + data);
                    }
                } else if (!baseKey.equals("class") && !baseKey.equals("args")) {
                    System.out.println("Unexpected key in JSON when loading ITPSearchSpace : " + baseKey);
                }
            }
        }
        return retValue;
    }

    @Override
    public T instantiate(int[] settings) {
        // we first need to update itp with the specified parameters, and then instantiate
        setTo(settings);
        return itp.instantiate();
    }

    /*
     * This method returns the default settings for the search space.
     * If the actual default value is not in the search space, then the value will be -1.
     */
    public int[] defaultSettings() {
        int[] settings = new int[searchDimensions.size()];
        Arrays.fill(settings, -1);
        for (int i = 0; i < searchDimensions.size(); i++) {
            Object defaultValue = itp.getDefaultParameterValue(searchDimensions.get(i));
            List<Object> possibleValues = values.get(i);
            for (int j = 0; j < possibleValues.size(); j++) {
                if (JSONUtils.areValuesEqual(possibleValues.get(j), defaultValue)) {
                    settings[i] = j;
                    break;
                }
            }
        }
        return settings;
    }

    public int[] settingsFromJSON(String fileName) {
        JSONObject json = JSONUtils.loadJSONFile(fileName);
        return settingsFromJSON(json);
    }

    // This will read in a JSON file and instantiate an agent from it
    // it will throw an error if the contents of the JSON file do not match the SearchSpace
    // The return value is settings that will instantiate the agent from the search space
    public int[] settingsFromJSON(JSONObject json) {

        int[] settings = new int[searchDimensions.size()];
        // We now iterate through all the tunable parameters and find the setting that corresponds
        // to the json content; if none provided then we use the default
        for (int i = 0; i < searchDimensions.size(); i++) {
            String fullParameterName = searchDimensions.get(i);
            String[] name = fullParameterName.split("\\.");
            Object value = json;
            for (String s : name) {
                value = ((JSONObject) value).get(s);
            }
            if (value == null) {
                // we use the default value
                value = itp.getDefaultParameterValue(fullParameterName);
            }
            if (value instanceof String str) {
                // if the value is a string, we need to check if it is a JSON file
                if (str.endsWith(".json")) {
                    // we load the JSON file and use that as the value
                    value = JSONUtils.loadClassFromFile(str);
                }
            }
            // we need to find the index of the value in the list of possible values
            List<Object> possibleValues = values.get(i);
            int index = -1;
            for (int j = 0; j < possibleValues.size(); j++) {
                if (JSONUtils.areValuesEqual(possibleValues.get(j), value)) {
                    index = j;
                    break;
                }
            }
            if (index == -1) {
                throw new AssertionError("Value " + value + " not found in possible values in search space for " + fullParameterName);
            }
            settings[i] = index;
        }

        // Then we check all the parameters in the JSON file are valid for the ITunableParameters
        // If any have values that are not the same as the searchspace values (or do not
        // match the default value for that parameter), then we throw an error
        // This is recursive as needed over any nested ITunableParameters in the JSON
        for (Object key : json.keySet()) {
            String keyName = (String) key;
            if (keyName.equals("class") || keyName.equals("args") || keyName.equals("budget") || searchDimensions.contains(keyName)) {
                continue;
            }
            Object value = json.get(keyName);
            // we do not check recursively here (possible future enhancement)
            if (value instanceof JSONObject)
                continue;
            // slightly awkward...TunableParameters has a rawJSON set of data that should be used to provide local overrides to the
            // global defaults specific to the main parameter definition
            Object defaultValue = itp instanceof TunableParameters<?> tp ? tp.getDefaultOverride(keyName) : itp.getDefaultParameterValue(keyName);
            if (!JSONUtils.areValuesEqual(value, defaultValue)) {
                throw new AssertionError("Value " + value + " for parameter " + keyName + " does not match default value " + defaultValue);
            }
        }
        return settings;
    }


    public JSONObject constructAgentJSON(int[] settings) {
        // we first need to update itp with the specified parameters, and then instantiate
        setTo(settings);
        Map<String, Integer> settingsMap = IntStream.range(0, settings.length).boxed().collect(toMap(this::name, i -> settings[i]));
        return itp.instanceToJSON(true, settingsMap);
    }

    @SuppressWarnings("unchecked")
    public void writeAgentJSON(int[] settings, String fileName) {
        JSONObject json = constructAgentJSON(settings);
        int budget = (int) itp.getParameterValue("budget");
        if (budget > 0)
            json.put("budget", budget);
        JSONUtils.writeJSON(json, fileName);
    }

    private void setTo(int[] settings) {
        for (int i = 0; i < settings.length; i++) {
            String pName = name(i);
            Object value = value(i, settings[i]);
            //   Object value = itp.getPossibleValues(pName).get(settings[i]);
            itp.setParameterValue(pName, value);
        }
    }

}
