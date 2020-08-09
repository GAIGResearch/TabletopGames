package players.rmhc;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import players.PlayerParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RMHCParams extends PlayerParameters implements ITunableParameters {
    public int horizon = 10;
    public double discountFactor = 0.9;

    public RMHCParams(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        RMHCParams copy = new RMHCParams(System.currentTimeMillis());
        copy.horizon = horizon;
        return copy;
    }

    @Override
    public HashMap<Integer, ArrayList<?>> getSearchSpace() {
        return new HashMap<Integer, ArrayList<?>>() {{
            put(0, new ArrayList<Integer>() {{ // Horizon
                add(6);
                add(8);
                add(10);
                add(12);
            }});
        }};
    }

    @Override
    public List<Integer> getParameterIds() {
        return new ArrayList<Integer>() {{
            add(0);
        }};
    }

    @Override
    public Object getDefaultParameterValue(int parameterId) {
        if (parameterId == 0) return 10;
        return null;
    }

    @Override
    public void setParameterValue(int parameterId, Object value) {
        if (parameterId == 0) horizon = (int) value;
        else System.out.println("Unknown parameter " + parameterId);
    }

    @Override
    public Object getParameterValue(int parameterId) {
        if (parameterId == 0) return horizon;
        else {
            System.out.println("Unknown parameter " + parameterId);
            return null;
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        if (parameterId == 0) return "Horizon";
        else {
            System.out.println("Unknown parameter " + parameterId);
            return null;
        }
    }
}
