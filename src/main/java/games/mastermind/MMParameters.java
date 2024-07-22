package games.mastermind;

import core.AbstractParameters;

import java.util.Objects;

public class MMParameters extends AbstractParameters {
    int boardWidth = 4;
    int boardHeight = 12;

    @Override
    protected AbstractParameters _copy() {
        MMParameters copy = new MMParameters();
        copy.boardWidth = boardWidth;
        copy.boardHeight = boardHeight;
        return copy;
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
}
