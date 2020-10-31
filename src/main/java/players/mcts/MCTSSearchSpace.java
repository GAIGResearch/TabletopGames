/*
package players.mcts;

import core.AbstractPlayer;
import evodef.AgentSearchSpace;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

*/
/**
 * Implements a SearchSpace for MCTSParams to enable optimisation
 *//*

public class MCTSSearchSpace extends AgentSearchSpace<AbstractPlayer> {

    // baseParams defines the default values to use for all parameters that are not being searched currently
    private MCTSParams baseParams;

    */
/**
     * Instantiates a SearchSpace from a json file.
     * An example such JSON File is:
     * {
     * 	"algorithm" : "MCTS",
     * 	"K" : [0.5, 1.0, 3.0, 10.0, 20.0, 50.0],
     * 	"rolloutLength" : [10, 20, 50, 100, 200, 500],
     * 	"rolloutsEnabled" : [false, true],
     * 	"rolloutType" : "Random",
     * 	"epsilon" : 1e-6
     * }
     *
     * In this example we are searching over K, rolloutLength and rolloutsEnabled.
     * All of these are arrays of the possible values.
     *
     * The other parameters (rolloutType and epsilon) have single values to be used in all cases.
     *
     * @param jsonFile
     *//*

    public MCTSSearchSpace(String jsonFile) {
        super(convertToSuperFormat(jsonFile));
        baseParams = MCTSParams.fromJSONFile(jsonFile);
    }

    @NotNull
    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> retValue = new HashMap<>();
        retValue.put("K", Double.class);
        retValue.put("rolloutLength", Integer.class);
        retValue.put("rolloutsEnabled", Boolean.class);
        retValue.put("rolloutType", String.class);
        retValue.put("epsilon", Double.class);
        retValue.put("budgetType", Integer.class);
        retValue.put("budget", Integer.class);
        return retValue;
    }

    */
/**
     * This generates an MCTSPlayer using the settings provided.
     * These settings must be indexed in exactly the same order as in the SearchSpace.
     *
     * This method should not be called directly outside of the NTBEA/Parameter Search framework.
     *
     * @param settings
     * @return
     *//*

    @Override
    public MCTSPlayer getAgent(@NotNull double[] settings) {
        Map<String, Object> settingsMap = settingsToMap(settings);
        MCTSParams params = (MCTSParams) baseParams.copy();
        params.K = (double) settingsMap.getOrDefault("K", params.K);
        params.rolloutLength = (int) settingsMap.getOrDefault("rolloutLength", params.rolloutLength);
        params.rolloutsEnabled = (boolean) settingsMap.getOrDefault("rolloutsEnabled", params.rolloutsEnabled);
        params.rolloutType = (String) settingsMap.getOrDefault("rolloutType", params.rolloutType);
        params.epsilon = (double) settingsMap.getOrDefault("epsilon", params.epsilon);
        params.budgetType = (int) settingsMap.getOrDefault("budgetType", params.budgetType);
        params.fmCallsBudget = (int) settingsMap.getOrDefault("budget", params.fmCallsBudget);
        params.timeBudget = (int) settingsMap.getOrDefault("budget", params.timeBudget);
        params.iterationsBudget = (int) settingsMap.getOrDefault("budget", params.iterationsBudget);
        return new MCTSPlayer(params);
    }

    */
/**
     * This helper method caters for the discrepancy between formats supported by the NTBEA library used and JSON.
     * The library expects a List<String> format, with each entry in the List being of the format:
     *   parameterName = value1, value2, value3 ...
     * This is then parsed to generate the set of possible values for each parameter.
     * This method converts a JSON format into this format as a List<String> object so that the superclass constructor
     * can be called.
     * @param jsonFile
     * @return
     *//*

    private static List<String> convertToSuperFormat(String jsonFile) {
        List<String> retValue = new ArrayList<>();
        try {
            FileReader reader = new FileReader(jsonFile);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            for (Object key : rawData.keySet()) {
                if (key instanceof String) {
                    if (MCTSParams.expectedKeys.contains(key)) {
                        Object data = rawData.get(key);
                        if (data instanceof JSONArray) {
                            // we have a set of options for this parameter
                            JSONArray arr = (JSONArray) data;
                            String results = key + "=" + arr.stream().map(Object::toString).collect(Collectors.joining(", ")) +
                                    "\n";
                            retValue.add(results);
                        }
                    } else {
                        System.out.println("Unexpected key in JSON for MCTSParameters : " + key);
                    }
                }
            }
            return retValue;

        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage() + " : problem loading MCTSSearchSpace from file " + jsonFile);
        }

    }
}
*/
