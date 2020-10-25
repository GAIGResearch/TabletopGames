package players.mcts;

import core.*;
import core.interfaces.ITunableParameters;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.io.FileReader;
import java.util.*;

public class MCTSParams extends PlayerParameters implements ITunableParameters {

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

    public AbstractPlayer getRolloutStrategy() {
        // For future expansion (used in Dominion)
        return new RandomPlayer(new Random(randomSeed));
    }

    public final static List<String> expectedKeys = Arrays.asList("algorithm", "seed", "K", "rolloutLength",
            "rolloutsEnabled", "epsilon", "rolloutType", "budgetType", "budget", "breakMS");

    @SuppressWarnings("unchecked")
    private static <T> T getParam(String name, JSONObject json, T defaultValue) {
        Object data = json.getOrDefault(name, defaultValue);
        if (data.getClass() == defaultValue.getClass())
            return (T) data;
        return defaultValue;
    }

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
}
