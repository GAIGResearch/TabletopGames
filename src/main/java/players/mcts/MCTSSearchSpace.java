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

public class MCTSSearchSpace extends AgentSearchSpace<AbstractPlayer> {

    private MCTSParams baseParams;

    public MCTSSearchSpace(String jsonFile) {
        super(convertToSuperFormat(jsonFile));
        baseParams = MCTSParams.fromJSONFile(jsonFile);
    }

    public MCTSSearchSpace(MCTSParams defaultParams, String searchSpaceFile) {
        super(searchSpaceFile);
        baseParams = defaultParams;
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
        return retValue;
    }

    @Override
    public MCTSPlayer getAgent(@NotNull double[] settings) {
        Map<String, Object> settingsMap = settingsToMap(settings);
        MCTSParams params = (MCTSParams) baseParams.copy();
        params.K = (double) settingsMap.getOrDefault("K", params.K);
        params.rolloutLength = (int) settingsMap.getOrDefault("rolloutLength", params.rolloutLength);
        params.rolloutsEnabled = (boolean) settingsMap.getOrDefault("rolloutsEnabled", params.rolloutsEnabled);
        params.rolloutType = (String) settingsMap.getOrDefault("rolloutType", params.rolloutType);
        params.epsilon = (double) settingsMap.getOrDefault("epsilon", params.epsilon);
        return new MCTSPlayer(params);
    }

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
