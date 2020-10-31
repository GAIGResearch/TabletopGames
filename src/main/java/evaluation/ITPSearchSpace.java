package evaluation;

import core.interfaces.ITunableParameters;
import evodef.AgentSearchSpace;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import players.mcts.MCTSParams;

import java.io.FileReader;
import java.util.*;
import java.util.stream.*;

/**
 * This is a wrapper around ITunableParameters<T> (the TAG standard) to implement the AgentSearchSpace
 * interface used by the NTBEA library.
 *
 */
public class ITPSearchSpace extends AgentSearchSpace<Object> {

    ITunableParameters itp;

    /**
     * Constructor of a SearchSpace to use all the default values defined by an
     * implementation of ITunableParameters
     *
     * @param tunableParameters The ITunableParameters object we want to optimise over.
     */
    public ITPSearchSpace(ITunableParameters tunableParameters) {
        super(convertToSuperFormat(tunableParameters.getJSONDescription(), tunableParameters));
        itp = tunableParameters;
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
        super(convertToSuperFormat(jsonFile, tunableParameters));
        itp = tunableParameters;
        validate();
    }
    public ITPSearchSpace(ITunableParameters tunableParameters, JSONObject json) {
        super(convertToSuperFormat(json, tunableParameters));
        itp = tunableParameters;
        validate();
    }

    @NotNull
    @Override
    public Map<String, Class<?>> getTypes() {
        // TODO: Fill in
        return null;
    }

    @Override
    public Object getAgent(@NotNull double[] doubles) {
        // TODO: Fill in
        return null;
    }

    private void validate() {
        // TODO: add validation checks between this.itp and this.getSearchDimensions()
        // TODO: If there are any parameters not defined in this.getSearchDimensions(), then use the default from itp
    }

    private static List<String> convertToSuperFormat(JSONObject json, ITunableParameters itp) {
        List<String> retValue = new ArrayList<>();
        for (Object key : json.keySet()) {
            if (key instanceof String) {
                if (itp.getParameterNames().contains(key)) {
                    Object data = json.get(key);
                    if (data instanceof JSONArray) {
                        // we have a set of options for this parameter
                        JSONArray arr = (JSONArray) data;
                        String results = key + "=" + arr.stream().map(Object::toString).collect(Collectors.joining(", ")) +
                                "\n";
                        retValue.add(results);
                    }
                } else {
                    System.out.println("Unexpected key in JSON when loading ITPSearchSpace : " + key);
                }
            }
        }
        return retValue;
    }

    private static List<String> convertToSuperFormat(String jsonFile, ITunableParameters itp) {
        try {
            FileReader reader = new FileReader(jsonFile);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            return convertToSuperFormat(rawData, itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage() + " : problem loading ITPSearchSpace from file " + jsonFile);
        }

    }

}
