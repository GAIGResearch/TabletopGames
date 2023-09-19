package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.components.Piece;
import utilities.Distance;
import utilities.Pair;
import utilities.Vector2D;

import java.util.Objects;

// TODO: can't move back and forth between the same 2 squares in 3 consecutive turns
public class NormalMove extends Move{

    // Dependent
    public final Vector2D displacement;

    // Independent
    public Vector2D destinationCoordinate;

    public NormalMove(Vector2D position, Vector2D displacement) {
        super(position);
        this.displacement = displacement.copy();
        this.destinationCoordinate = null;
    }

    public NormalMove(int movedPieceID, Vector2D destinationCoordinate) {
        super(movedPieceID);
        this.destinationCoordinate = destinationCoordinate.copy();
        this.displacement = null;
    }

    private NormalMove(Vector2D position, int movePieceID, Vector2D destinationCoordinate, Vector2D displacement) {
        super(position, movePieceID);
        this.destinationCoordinate = destinationCoordinate != null? destinationCoordinate.copy() : null;
        this.displacement = displacement != null? displacement.copy() : null;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        GridBoard board = ((StrategoGameState)gs).getGridBoard();
        Piece movedPiece = getPiece((StrategoGameState) gs);

        board.setElement(movedPiece.getPiecePosition().getX(), movedPiece.getPiecePosition().getY(), null);
        if (destinationCoordinate == null) {
            destinationCoordinate = position.add(displacement);
        }

        board.setElement(destinationCoordinate.getX(), destinationCoordinate.getY(), movedPiece);

        if (movedPiece.getPieceType() == Piece.PieceType.SCOUT &&
                Distance.manhattan_distance(destinationCoordinate, movedPiece.getPiecePosition()) >
                        ((StrategoParams)gs.getGameParameters()).moveSpeed) {
            // Piece revealed itself to be scout
            movedPiece.setPieceKnown(true);
        }
        movedPiece.setPiecePosition(destinationCoordinate);

        return true;
    }

    @Override
    public NormalMove copy() {
        return new NormalMove(position, movedPieceID, destinationCoordinate != null? destinationCoordinate.copy() : null, displacement);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        if (position == null) {
            return "Move " + movedPieceID + " -> " + destinationCoordinate.toString();
        } else {
            Pair<Vector2D.Direction, Integer> direction = Vector2D.Direction.approxVecToDir(displacement);
            if (direction != null) {
                return "Move from " + position + " " + direction;
            } else return "Move from " + position + " " + displacement;
        }
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
        return Objects.equals(displacement, that.displacement) && Objects.equals(destinationCoordinate, that.destinationCoordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displacement, destinationCoordinate);
    }

    @Override
    public Vector2D to(StrategoGameState gs) {
        if (destinationCoordinate != null) {
            return destinationCoordinate;
        }
        return position.add(displacement);
    }
}