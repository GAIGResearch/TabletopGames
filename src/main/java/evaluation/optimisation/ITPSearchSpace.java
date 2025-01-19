package evaluation.optimisation;

import core.interfaces.ITunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import utilities.Pair;
import evaluation.optimisation.ntbea.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * This is a wrapper around ITunableParameters<T> (the TAG standard) to implement the AgentSearchSpace
 * interface used by the NTBEA library.
 */
public class ITPSearchSpace<T> extends AgentSearchSpace<T> {

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
        List<List<Object>> allPossibleValues = new ArrayList<>();
        for (String name : parameterNames) {
            List<Object> possibleValues = itp.getPossibleValues(name);
            allPossibleValues.add(possibleValues);
        }
        super.initialise(parameterNames, allPossibleValues);
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
        Map<String, Class<?>> parameterTypes = allParameterTypesWithRecursion(json);
        List<List<Object>> allPossibleValues = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();
        for (String nameAndValues : parameterTypes.keySet()) {
            // Now get possible values from JSON
            Class<?> clazz = parameterTypes.get(nameAndValues);
            String[] firstSplit = nameAndValues.split("=");
            if (firstSplit.length != 2)
                throw new AssertionError("Unexpected format of nameAndValues " + nameAndValues);
            String name = firstSplit[0];
            String[] values = firstSplit[1].split(", ");

            List<Object> possibleValues = new ArrayList<>();
            for (String v : values) {
                String strValue = v.trim();
                if (clazz.isEnum()) {
                    Class<? extends Enum> enumCl = (Class<? extends Enum>) clazz;
                    possibleValues.add(Enum.valueOf(enumCl, strValue));
                } else if (clazz == Double.class || clazz == double.class) {
                    possibleValues.add(Double.valueOf(strValue));
                } else if (clazz == Integer.class || clazz == int.class) {
                    possibleValues.add(Integer.valueOf(strValue));
                } else if (clazz == Long.class || clazz == long.class) {
                    possibleValues.add(Long.valueOf(strValue).intValue());
                } else if (clazz == Boolean.class || clazz == boolean.class) {
                    possibleValues.add(Boolean.valueOf(strValue));
                } else if (clazz == String.class) {
                    possibleValues.add(strValue);
                } else {
                    throw new AssertionError("Unsupported Clazz in Parameter Tuning " + clazz);
                }
            }

            allPossibleValues.add(possibleValues);
            parameterNames.add(name);
        }
        super.initialise(parameterNames, allPossibleValues);
        if (tunableParameters instanceof TunableParameters tp) {
            tp.setRawJSON(json);
        }
    }

    @SuppressWarnings("unchecked")
/**
 * This returns a List of Pairs. Each Pair contains a String that is made up of:
 *      - the name of the parameter that can vary (with namespace prepended where there has been recursion),
 *      - "="
 *      - and a comma delimited list of the values this parameter can take
 * Then the Class of the parameter as the second member of the Pair.
 * // TODO: This was mandated by old library usage. Can now be refactored to avoid need
 * // to split on ",", and hence allow arbitrary classes as tunable moieties.
 */
    private static List<Pair<String, Class<?>>> extractRecursiveParameters(String nameSpace, JSONObject json, ITunableParameters itp) {
        List<Pair<String, Class<?>>> retValue = new ArrayList<>();
        for (Object baseKey : json.keySet()) {
            if (baseKey instanceof String) {
                if (itp.getParameterNames().contains(baseKey)) {
                    Object data = json.get(baseKey);
                    String key = "".equals(nameSpace) ? (String) baseKey : nameSpace + "." + baseKey;
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
                            throw new AssertionError(e.getMessage() + " problem creating SearchSpace " + data);
                        }

                    } else if (data instanceof JSONArray arr) {
                        // we have a set of options for this parameter
                        String results = key + "=" + arr.stream().map(Object::toString).collect(Collectors.joining(", ")) +
                                "\n";
                        if (debug)
                            System.out.println(baseKey + " : adding " + results);
                        retValue.add(new Pair<>(results, itp.getDefaultParameterValue((String) baseKey).getClass()));
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

    protected Map<String, Class<?>> allParameterTypesWithRecursion(JSONObject json) {
        return extractRecursiveParameters("", json, itp).stream()
                .collect(
                        toMap(p -> p.a, p -> p.b)
                );
    }

    @Override
    public T instantiate(int[] settings) {
        // we first need to update itp with the specified parameters, and then instantiate
        setTo(settings);
        return itp.instantiate();
    }

    public JSONObject getAgentJSON(int[] settings) {
        // we first need to update itp with the specified parameters, and then instantiate
        setTo(settings);
        return itp.instanceToJSON(true);
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
