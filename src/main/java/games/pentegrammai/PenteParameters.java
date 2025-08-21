package games.pentegrammai;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

public class PenteParameters extends TunableParameters {

    public int boardSize = 10;
    public int dieSides = 6;
    public int[] sacredPoints = {2, 7};
    public boolean kiddsVariant = false;

    public PenteParameters() {
        addTunableParameter("boardSize", 10);
        addTunableParameter("dieSides", 6, List.of(4, 6, 10));
        addTunableParameter("kiddsVariant", false, List.of(false, true));
    }

    @Override
    public PenteParameters instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        boardSize = (int) getParameterValue("boardSize");
        dieSides = (int) getParameterValue("dieSides");
        kiddsVariant = (boolean) getParameterValue("kiddsVariant");
        sacredPoints = new int[]{boardSize / 4, 3 * boardSize / 4}; // default sacred points
    }

    @Override
    protected PenteParameters _copy() {
        PenteParameters copy = new PenteParameters();
        copy.sacredPoints = Arrays.copyOf(this.sacredPoints, this.sacredPoints.length);
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PenteParameters other)) return false;
        return Arrays.equals(sacredPoints, other.sacredPoints);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Arrays.hashCode(sacredPoints);
    }
}
