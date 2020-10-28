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
 * This is a wrapper around ITunableParameters<T> (the TAG standard) to the AgentSearchSpace
 * interface used by the NTBEA library.
 *
 * @param <T> This should either be (a subclass of) AbstractPlayer or (a subclass of) Game.
 *
 */
public class ITPSearchSpace<T> extends AgentSearchSpace<T> {

    ITunableParameters<T> itp;

    public ITPSearchSpace(ITunableParameters<T> tunableParameters) {
        // NO No NO. List<String> is the format of the "blah"="values"
        super(convertToSuperFormat(tunableParameters.getJSONDescription()));
        itp = tunableParameters;
        // TODO: Fill in all the methods.
    }

    @NotNull
    @Override
    public Map<String, Class<?>> getTypes() {
        return null;
    }

    @Override
    public T getAgent(@NotNull double[] doubles) {
        return null;
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
