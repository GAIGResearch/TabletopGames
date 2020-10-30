package games.tictactoe;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;

import java.util.*;

public class TicTacToeGameParameters extends AbstractParameters {
    public int gridSize = 3;

    public TicTacToeGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        TicTacToeGameParameters tttgp = new TicTacToeGameParameters(System.currentTimeMillis());
        tttgp.gridSize = gridSize;
        return tttgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicTacToeGameParameters)) return false;
        if (!super.equals(o)) return false;
        TicTacToeGameParameters that = (TicTacToeGameParameters) o;
        return gridSize == that.gridSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize);
    }

    @Override
    public Map<Integer, ArrayList<?>> getSearchSpace() {
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

    @Override
    public TicTacToeGame instantiate() {
        return new TicTacToeGame(this);
    }

}
