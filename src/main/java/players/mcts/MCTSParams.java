package players.mcts;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import players.utils.PlayerParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MCTSParams extends PlayerParameters implements ITunableParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = false;
    public double epsilon = 1e-6;

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
        }};
    }

    @Override
    public List<Integer> getParameterIds() {
        return new ArrayList<Integer>() {{
            add(0);
            add(1);
            add(2);
        }};
    }

    @Override
    public Object getDefaultParameterValue(int parameterId) {
        if (parameterId == 0) return Math.sqrt(2);
        else if (parameterId == 1) return 10;
        else if (parameterId == 2) return false;
        return null;
    }

    @Override
    public void setParameterValue(int parameterId, Object value) {
        if (parameterId == 0) K = (double) value;
        else if (parameterId == 1) rolloutLength = (int) value;
        else if (parameterId == 2) rolloutsEnabled = (boolean) value;
        else System.out.println("Unknown parameter " + parameterId);
    }

    @Override
    public Object getParameterValue(int parameterId) {
        if (parameterId == 0) return K;
        else if (parameterId == 1) return rolloutLength;
        else if (parameterId == 2) return rolloutsEnabled;
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
        else {
            System.out.println("Unknown parameter " + parameterId);
            return null;
        }
    }
}
