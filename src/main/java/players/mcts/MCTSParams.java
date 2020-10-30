package players.mcts;

import core.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.io.FileReader;
import java.util.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = false;
    public double epsilon = 1e-6;
    public String rolloutType = "Random";

    public MCTSParams(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        MCTSParams params = new MCTSParams(System.currentTimeMillis());
        params.K = K;
        params.rolloutLength = rolloutLength;
        params.rolloutsEnabled = rolloutsEnabled;
        params.epsilon = epsilon;
        return params;
    }

    @Override
    public HashMap<Integer, ArrayList<?>> getSearchSpace() {
        return new HashMap<Integer, ArrayList<?>>() {{
            put(0, new ArrayList<Double>() {{ // K
                add(1.0);
                add(Math.sqrt(2));
                add(2.0);
            }});
            put(1, new ArrayList<Integer>() {{ // Rollout length
                add(6);
                add(8);
                add(10);
                add(12);
            }});
            put(2, new ArrayList<Boolean>() {{ // Rollouts enabled
                add(false);
                add(true);
            }});
            put(3, new ArrayList<String>() {{
                add("Random");
            }});
        }};
    }

    @Override
    public List<Integer> getParameterIds() {
        return new ArrayList<Integer>() {{
            add(0);
            add(1);
            add(2);
            add(3);
        }};
    }

    @Override
    public Object getDefaultParameterValue(int parameterId) {
        switch (parameterId) {
            case 0:
                return Math.sqrt(2);
            case 1:
                return 10;
            case 2:
                return false;
            case 3:
                return "Random";
        }
        return null;
    }

    @Override
    public void setParameterValue(int parameterId, Object value) {
        if (parameterId == 0) K = (double) value;
        else if (parameterId == 1) rolloutLength = (int) value;
        else if (parameterId == 2) rolloutsEnabled = (boolean) value;
        else if (parameterId == 3) rolloutType = (String) value;
        else System.out.println("Unknown parameter " + parameterId);
    }

    @Override
    public Object getParameterValue(int parameterId) {
        if (parameterId == 0) return K;
        else if (parameterId == 1) return rolloutLength;
        else if (parameterId == 2) return rolloutsEnabled;
        else if (parameterId == 3) return rolloutType;
        else {
            System.out.println("Unknown parameter " + parameterId);
            return null;
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        if (parameterId == 0) return "K";
        else if (parameterId == 1) return "Rollout length";
        else if (parameterId == 2) return "Rollouts enabled";
        else if (parameterId == 3) return "Rollout type";
        else {
            System.out.println("Unknown parameter " + parameterId);
            return null;
        }
    }

    /**
     * @return Returns the AbstractPlayer policy that will take actions during an MCTS rollout.
     *         This defaults to a Random player.
     */
    public AbstractPlayer getRolloutStrategy() {
        return new RandomPlayer(new Random(randomSeed));
    }

    /**
     * The set of keys that can be in the JSON file. If we find anything not in this list we log the fact to the console
     *  to highlight potential typos
     */
    public final static List<String> expectedKeys = Arrays.asList("algorithm", "seed", "K", "rolloutLength",
            "rolloutsEnabled", "epsilon", "rolloutType", "budgetType", "budget", "breakMS");

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

    /**
     * Instantiate parameters from a JSON file
     *
     * @param filename The file with the JSON format data
     * @return The full set of parameters extracted from the file
     */
    public static MCTSParams fromJSONFile(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            return fromJSON(rawData);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage() + " : problem loading MCTSParams from file " + filename);
        }
    }

    /**
     * Instantiate paramaters from a JSONObject
     */
    public static MCTSParams fromJSON(JSONObject rawData) {
        long seed = getParam("seed", rawData, System.currentTimeMillis());
        MCTSParams retValue = new MCTSParams(seed);

        retValue.K = getParam("K", rawData, retValue.K);
        retValue.rolloutLength = getParam("rolloutLength", rawData, retValue.rolloutLength);
        retValue.rolloutsEnabled = getParam("rolloutsEnabled", rawData, retValue.rolloutsEnabled);
        retValue.epsilon = getParam("epsilon", rawData, retValue.epsilon);
        retValue.rolloutType = getParam("rolloutType", rawData, retValue.rolloutType);
        retValue.budgetType = getParam("budgetType", rawData, retValue.budgetType);
        int budget = getParam("budget", rawData, -1);
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
        }

        // We should also check that there are no other properties in there
        for (Object key : rawData.keySet()) {
            if (key instanceof String && !expectedKeys.contains(key)) {
                System.out.println("Unexpected key in JSON for MCTSParameters : " + key);
            }
        }

        return retValue;
    }

    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }
}
