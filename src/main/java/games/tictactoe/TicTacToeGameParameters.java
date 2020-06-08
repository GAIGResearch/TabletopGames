package games.tictactoe;

import core.AbstractGameParameters;
import core.interfaces.TunableParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TicTacToeGameParameters extends AbstractGameParameters implements TunableParameters {
    public int gridSize = 3;

    public TicTacToeGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        TicTacToeGameParameters tttgp = new TicTacToeGameParameters(System.currentTimeMillis());
        tttgp.gridSize = gridSize;
        return tttgp;
    }

    @Override
    public HashMap<Integer, ArrayList<?>> getSearchSpace() {
        return new HashMap<Integer, ArrayList<?>>() {{
            put(0, new ArrayList<Integer>() {{
                add(3);
                add(4);
                add(5);
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
        if (parameterId == 0) return 3;
        return null;
    }

    @Override
    public void setParameterValue(int parameterId, Object value) {
        if (parameterId == 0) gridSize = (int) value;
        else System.out.println("Unknown parameter " + parameterId);
    }

    @Override
    public Object getParameterValue(int parameterId) {
        if (parameterId == 0) return gridSize;
        return null;
    }

    @Override
    public String getParameterName(int parameterId) {
        if (parameterId == 0) return "Grid size";
        return null;
    }

}
