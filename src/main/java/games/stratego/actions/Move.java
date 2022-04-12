package games.stratego.actions;

import core.actions.AbstractAction;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;

import java.util.Arrays;
import java.util.Objects;

public abstract class Move extends AbstractAction {

    protected final int movedPieceID;

    protected Move(int movedPieceID) {
        this.movedPieceID = movedPieceID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return movedPieceID == move.movedPieceID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movedPieceID);
    }

    public int getMovedPieceID() {
        return movedPieceID;
    }

    public int[] from(StrategoGameState gs) {
        Piece movedPiece = (Piece) gs.getComponentById(movedPieceID);
        return movedPiece.getPiecePosition();
    }
    public abstract int[] to(StrategoGameState gs);

    public abstract String getPOString(StrategoGameState gs);

}
