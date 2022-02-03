package games.stratego.actions;

import core.actions.AbstractAction;

import java.util.Arrays;
import java.util.Objects;

public abstract class Move extends AbstractAction {

    protected final int movedPieceID;
    protected final int[] destinationCoordinate;

    protected Move(int movedPieceID, int[] destinationCoordinate) {
        this.movedPieceID = movedPieceID;
        this.destinationCoordinate = destinationCoordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return movedPieceID == move.movedPieceID && Arrays.equals(destinationCoordinate, move.destinationCoordinate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(movedPieceID);
        result = 31 * result + Arrays.hashCode(destinationCoordinate);
        return result;
    }

    public int getMovedPieceID() {
        return movedPieceID;
    }

    public int[] getDestinationCoordinate() {
        return destinationCoordinate;
    }
}
