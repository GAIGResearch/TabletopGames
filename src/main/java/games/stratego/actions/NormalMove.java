package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.components.Piece;
import utilities.Distance;
import utilities.Vector2D;

import java.util.Arrays;
import java.util.Objects;

// TODO: can't move back and forth between the same 2 squares in 3 consecutive turns
public class NormalMove extends Move{

    // Dependent
    public final Vector2D direction;

    // Independent
    public final Vector2D destinationCoordinate;

    public NormalMove(Vector2D position, Vector2D direction) {
        super(position);
        this.direction = direction.copy();
        this.destinationCoordinate = null;
    }

    public NormalMove(int movedPieceID, Vector2D destinationCoordinate) {
        super(movedPieceID);
        this.destinationCoordinate = destinationCoordinate.copy();
        this.direction = null;
    }

    private NormalMove(Vector2D position, int movePieceID, Vector2D destinationCoordinate, Vector2D direction) {
        super(position, movePieceID);
        this.destinationCoordinate = destinationCoordinate != null? destinationCoordinate.copy() : null;
        this.direction = direction != null? direction.copy() : null;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Piece movedPiece = getPiece((StrategoGameState) gs);
        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();

        board.setElement(movedPiece.getPiecePosition().getX(), movedPiece.getPiecePosition().getY(), null);
        Vector2D destination;
        if (destinationCoordinate != null) {
            destination = destinationCoordinate;
        } else {
            destination = position.add(direction);
        }
        board.setElement(destination.getX(), destination.getY(), movedPiece);

        if (movedPiece.getPieceType() == Piece.PieceType.SCOUT &&
                Distance.manhattan_distance(destination, movedPiece.getPiecePosition()) >
                        ((StrategoParams)gs.getGameParameters()).moveSpeed) {
            // Piece revealed itself to be scout
            movedPiece.setPieceKnown(true);
        }
        movedPiece.setPiecePosition(destination);

        return true;
    }

    @Override
    public NormalMove copy() {
        return new NormalMove(position, movedPieceID, destinationCoordinate, direction);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Piece movedPiece = getPiece((StrategoGameState) gameState);
        return "Move (" + movedPieceID + ": " + movedPiece.getPiecePosition().toString() + " -> " +
                destinationCoordinate.toString() + ")";
    }

    @Override
    public String getPOString(StrategoGameState gameState) {
        Piece movedPiece = getPiece(gameState);
        return "Move (" + movedPiece.getPiecePosition().toString() + " -> " +
                destinationCoordinate.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NormalMove)) return false;
        if (!super.equals(o)) return false;
        NormalMove that = (NormalMove) o;
        return Objects.equals(direction, that.direction) && Objects.equals(destinationCoordinate, that.destinationCoordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), direction, destinationCoordinate);
    }

    @Override
    public Vector2D to(StrategoGameState gs) {
        if (destinationCoordinate != null) {
            return destinationCoordinate;
        }
        return position.add(direction);
    }
}