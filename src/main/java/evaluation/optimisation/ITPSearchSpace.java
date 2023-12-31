package evaluation.optimisation;

import core.interfaces.ITunableParameters;
import evodef.AgentSearchSpace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utilities.JSONUtils;
import utilities.Pair;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * This is a wrapper around ITunableParameters<T> (the TAG standard) to implement the AgentSearchSpace
 * interface used by the NTBEA library.
 */
public class ITPSearchSpace extends AgentSearchSpace<Object> {

    static boolean debug = false;
    ITunableParameters itp;
    Map<Integer, String> tunedIndexToParameterName = new HashMap<>();

    /**
     * Constructor of a SearchSpace to use all the default values defined by an
     * implementation of ITunableParameters
     *
     * @param tunableParameters The ITunableParameters object we want to optimise over.
     */
    public ITPSearchSpace(ITunableParameters tunableParameters) {
        super(convertToSuperFormatString(tunableParameters.getJSONDescription(), tunableParameters), tunableParameters.getParameterTypes());
        initialiseITP(tunableParameters);
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
    public ITPSearchSpace(ITunableParameters tunableParameters, String jsonFile) {
        super(convertToSuperFormatFile(jsonFile, tunableParameters), tunableParameters.getParameterTypes());
        initialiseITP(tunableParameters);
    }

    public ITPSearchSpace(ITunableParameters tunableParameters, JSONObject json) {
        super(convertToSuperFormatJSON(json, tunableParameters),
                allParameterTypesWithRecursion(json, tunableParameters));
        initialiseITP(tunableParameters);
    }

    @SuppressWarnings("unchecked")
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
                            Object child = registerChild(key, subJSON);
                            itp.setParameterValue((String) baseKey, child);
                            if (child instanceof ITunableParameters)
                                retValue.addAll(extractRecursiveParameters(key, (JSONObject) data, (ITunableParameters) child));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new AssertionError(e.getMessage() + " problem creating SearchSpace " + data);
                        }

                    } else if (data instanceof JSONArray) {
                        // we have a set of options for this parameter
                        JSONArray arr = (JSONArray) data;
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
                } else if (!baseKey.equals("class")) {
                    System.out.println("Unexpected key in JSON when loading ITPSearchSpace : " + baseKey);
                }
            }
        }
        return retValue;
    }

    public static Object registerChild(String nameSpace, JSONObject json) {
        try {
            if (debug)
                System.out.println("Registering " + nameSpace);
            Object child = JSONUtils.loadClassFromJSON(json);
            if (child instanceof TunableParameters) {
                TunableParameters tunableChild = (TunableParameters) child;
                TunableParameters.loadFromJSON(tunableChild, json);
            }
            return child;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage() + " when trying to a nested TunableParameters : " + json.get("class"));
        }
    }


    private static Map<String, Class<?>> allParameterTypesWithRecursion(JSONObject json, ITunableParameters itp) {
        return extractRecursiveParameters("", json, itp).stream()
                .collect(
                        toMap(p -> p.a.split("=")[0], p -> p.b)
                );
    }

    private static List<String> convertToSuperFormatJSON(JSONObject json, ITunableParameters itp) {
        return extractRecursiveParameters("", json, itp).stream().map(p -> p.a).collect(toList());
    }

    private static List<String> convertToSuperFormatString(String json, ITunableParameters itp) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(json);
            return convertToSuperFormatJSON(rawData, itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getClass() + " : " + e.getMessage() + " : problem loading ITPSearchSpace from String " + json);
        }

    }

    private static List<String> convertToSuperFormatFile(String jsonFile, ITunableParameters itp) {
        try {
            FileReader reader = new FileReader(jsonFile);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            return convertToSuperFormatJSON(rawData, itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage() + " : problem loading ITPSearchSpace from file " + jsonFile);
        }

    }

    private void initialiseITP(ITunableParameters tunableParameters) {
        itp = tunableParameters;
        tunedIndexToParameterName = IntStream.range(0, nDims()).boxed()
                .collect(toMap(i -> i, i -> getSearchKeys().get(i)));
    }

    public int getIndexOf(String parameter) {
        if (!getSearchKeys().contains(parameter))
            return -1;
        return getSearchKeys().indexOf(parameter);
    }

    public Object getAgent(int[] settings) {
        // we first need to update itp with the specified parameters, and then instantiate
        for (int i = 0; i < settings.length; i++) {
            String pName = tunedIndexToParameterName.get(i);
            Object value = value(i, settings[i]);
            //   Object value = itp.getPossibleValues(pName).get(settings[i]);
            itp.setParameterValue(pName, value);
        }
        return itp.instantiate();
    }

}
