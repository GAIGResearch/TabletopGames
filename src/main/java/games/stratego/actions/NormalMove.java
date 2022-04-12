package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.components.Piece;
import utilities.Distance;

import java.util.Arrays;

// TODO: can't move back and forth between the same 2 squares in 3 consecutive turns
public class NormalMove extends Move{

    public final int[] destinationCoordinate;

    public NormalMove(int movedPieceID, int[] destinationCoordinate) {
        super(movedPieceID);
        this.destinationCoordinate = destinationCoordinate.clone();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Piece movedPiece = (Piece) gs.getComponentById(movedPieceID);
        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();

        board.setElement(movedPiece.getPiecePosition()[0], movedPiece.getPiecePosition()[1], null);
        board.setElement(destinationCoordinate[0], destinationCoordinate[1], movedPiece);

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
        return new NormalMove(movedPieceID, destinationCoordinate.clone());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Piece movedPiece = (Piece) gameState.getComponentById(movedPieceID);
        return "Move (" + movedPieceID + ": " + Arrays.toString(movedPiece.getPiecePosition()) + " -> " +
                Arrays.toString(destinationCoordinate) + ")";
    }

    @Override
    public String getPOString(StrategoGameState gameState) {
        Piece movedPiece = (Piece) gameState.getComponentById(movedPieceID);
        return "Move (" + Arrays.toString(movedPiece.getPiecePosition()) + " -> " +
                Arrays.toString(destinationCoordinate) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NormalMove that = (NormalMove) o;
        return Arrays.equals(destinationCoordinate, that.destinationCoordinate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(destinationCoordinate);
        return result;
    }

    @Override
    public int[] to(StrategoGameState gs) {
        return destinationCoordinate;
    }
}