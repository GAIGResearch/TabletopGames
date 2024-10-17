package games.mastermind;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;

public class MMParameters extends TunableParameters {
    public int boardWidth = 4;
    public int boardHeight = 12;

    public MMParameters() {
        addTunableParameter("boardWidth", 4, Arrays.asList(2,3,4,5,6,7));
        addTunableParameter("boardHeight", 12, Arrays.asList(5,6,7,8,9,10,11,12));
        _reset();
    }

    @Override
    protected AbstractParameters _copy() {
        return new MMParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MMParameters that = (MMParameters) o;
        return boardWidth == that.boardWidth && boardHeight == that.boardHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boardWidth, boardHeight);
    }

    @Override
    public Object instantiate() {
        return GameType.Mastermind.createGameInstance(1, this);
    }

    @Override
    public void _reset() {
        boardWidth = (int) getParameterValue("boardWidth");
        boardHeight = (int) getParameterValue("boardHeight");
    }
}
