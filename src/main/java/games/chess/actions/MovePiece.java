package games.chess.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.chess.ChessGameState;
import games.chess.components.ChessPiece;

import java.util.Objects;

public class MovePiece extends AbstractAction {

    private final int startX;
    private final int startY;
    private final int targetX;
    private final int targetY;

    public MovePiece(int sx, int sy, int tx, int ty) {
        this.startX = sx;
        this.startY = sy;
        this.targetX = tx;
        this.targetY = ty;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        ChessGameState gs = (ChessGameState) ags;
        ChessPiece piece = gs.getPiece(startX, startY);

        //Delete the piece in the start position
        gs.deletePiece(piece);

        // Set the moved flag to true for the piece being moved.
        if (piece.getMoved() == ChessPiece.MovedState.NOT_MOVED) {
            piece.setMoved(ChessPiece.MovedState.MOVED);
        } 

        // Set en passant flag if the piece is a pawn and the target position is two squares forward
        if (piece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN) {
            if (Math.abs(targetY - startY) == 2) {
                piece.setEnPassant(true);
            }
        }

        // Check if the target position is empty or occupied by an opponent's piece
        ChessPiece targetPiece = gs.getPiece(targetX, targetY);

        gs.incrementHalfMoveClock(); // Increment the half-move clock for the current player

        if (targetPiece != null && targetPiece.getOwnerId() == 1-piece.getOwnerId()) {
            gs.deletePiece(targetPiece);// Capture(remove) the opponent's piece
            gs.resetHalfMoveClock(); // Reset the half-move clock when a piece is captured
            // System.out.println("Captured piece at (" + targetX + ", " + targetY + ")");
        }

        // Reset the half-move clock if the moved piece is a pawn

        if (piece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN) {
            gs.resetHalfMoveClock();
        }
        
        gs.setPiece(targetX, targetY, piece); // Move the piece to the target position

        return true;
    }

    @Override
    public MovePiece copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MovePiece)) return false;
        MovePiece other = (MovePiece) obj;
        return startX == other.startX && startY == other.startY && targetX == other.targetX && targetY == other.targetY;

    }

    @Override
    public int hashCode() {
        return Objects.hash(startX, startY, targetX, targetY);
    }

    @Override
    public String toString() {
        return "MovePiece{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", targetX=" + targetX +
                ", targetY=" + targetY +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        ChessGameState gs = (ChessGameState) gameState;
        ChessPiece startPiece = gs.getPiece(startX, startY);
        ChessPiece targetPiece = gs.getPiece(targetX, targetY);
        String startPieceName = startPiece != null ? startPiece.getChessPieceType().toString() : "empty";
        String targetPieceName = targetPiece != null ? targetPiece.getChessPieceType().toString() : "empty";
        String startSquare = gs.getChessCoordinates(startX, startY);
        String targetSquare = gs.getChessCoordinates(targetX, targetY);
        String output = "Move " + startPieceName + " from " + startSquare + " to " + targetSquare;

        if (targetPiece != null) {
            output += ", capturing " + targetPieceName;
        }
        return output;
    }
    
}
