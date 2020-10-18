package players.mcts;

import core.*;
import core.interfaces.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.io.*;
import java.util.*;

public class MCTSParams extends PlayerParameters implements ITunableParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = true;
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
        return new RandomPlayer(new Random(randomSeed));
    }

    public static MCTSParams fromJSON(String filename) {
        List<String> expectedKeys = Arrays.asList("seed", "K", "rolloutLength", "rolloutsEnabled", "epsilon", "rolloutType");

        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            long seed = (long) rawData.getOrDefault("seed", System.currentTimeMillis());
            MCTSParams retValue = new MCTSParams(seed);

            retValue.K = (double) rawData.getOrDefault("K", retValue.K);
            retValue.rolloutLength = ((Long) rawData.getOrDefault("rolloutLength", retValue.rolloutLength)).intValue();
            retValue.rolloutsEnabled = (boolean) rawData.getOrDefault("rolloutsEnabled", retValue.rolloutsEnabled);
            retValue.epsilon = (double) rawData.getOrDefault("epsilon", retValue.epsilon);
            retValue.rolloutType = (String) rawData.getOrDefault("rolloutType", retValue.rolloutType);

            // We should also check that there are no other properties in there
            for (Object key : rawData.keySet()) {
                if (key instanceof String && !expectedKeys.contains(key)) {
                    System.out.println("Unexpected key in JSON for MCTSParameters : " + key);
                }
            }

            return retValue;

        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getMessage() + " : problem loading MCTSParams from file " + filename);
        }
    }
}
