package games.stratego.actions;

import core.actions.AbstractAction;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import utilities.Vector2D;

import java.util.Objects;

public abstract class Move extends AbstractAction {
    // Dependent:
    public final Vector2D position;

    // Independent:
    public final int movedPieceID;

    protected Move(Vector2D position) {
        this.position = position.copy();
        this.movedPieceID = -1;
    }

    protected Move(int movedPieceID) {
        this.movedPieceID = movedPieceID;
        this.position = null;
    }

    protected Move(Vector2D position, int movedPieceID) {
        this.movedPieceID = movedPieceID;
        this.position = position != null? position.copy() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return movedPieceID == move.movedPieceID && Objects.equals(position, move.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, movedPieceID);
    }

    public int getMovedPieceID() {
        return movedPieceID;
    }

    public Vector2D from(StrategoGameState gs) {
        if (position == null) {
            return ((Piece) gs.getComponentById(movedPieceID)).getPiecePosition();
        }
        return position;
    }

    public Piece getPiece(StrategoGameState gs) {
        if (position != null) {
            return (Piece) gs.getGridBoard().getElement(position.getX(), position.getY());
        } else {
            return (Piece) gs.getComponentById(movedPieceID);
        }
    }

    public abstract Vector2D to(StrategoGameState gs);

    public abstract String getPOString(StrategoGameState gs);

}
