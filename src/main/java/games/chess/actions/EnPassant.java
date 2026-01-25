package games.chess.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.chess.ChessGameState;
import games.chess.components.ChessPiece;
import java.lang.System;
import java.util.Objects;

/**
    * EnPassant action for chess game. When a pawn moves two squares forward from its starting position, it can be captured by an opponent's pawn as if it had only moved one square. This action captures the opponent's pawn and moves the current player's pawn to the target position.
    * The target position is one square diagonally behind the opponent's pawn. This action is only available immediately after the opponent's pawn moves two squares forward.
 */
public class EnPassant extends AbstractAction {

    private final int startX;
    private final int startY;
    private final int targetX;

    public EnPassant(int sx, int sy, int tx) {
        this.startX = sx;
        this.startY = sy;
        this.targetX = tx;
    }
    @Override
    public boolean execute(AbstractGameState ags) {
        ChessGameState gs = (ChessGameState) ags;
        ChessPiece piece = gs.getPiece(startX, startY);
        int direction = (piece.getOwnerId() == 0) ? 1 : -1; // Determine the direction based on the owner ID
        ChessPiece targetPiece = gs.getPiece(targetX, startY); // Get the pawn that is being captured
        int targetY = startY+direction; // The target Y position for the en passant move

        //Delete the piece in the start position
        gs.deletePiece(piece); // Remove the piece from its original position
        gs.incrementHalfMoveClock(); // Increment the half-move clock for the current player
        gs.deletePiece(targetPiece);// Capture(remove) the opponent's piece
        gs.resetHalfMoveClock(); // Reset the half-move clock when a piece is captured
        
        gs.setPiece(targetX, targetY, piece); // Move the piece to the target position

        return true;
    }

    @Override
    public EnPassant copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EnPassant)) return false;
        EnPassant other = (EnPassant) obj;
        return startX == other.startX && startY == other.startY && targetX == other.targetX;

    }

    @Override
    public int hashCode() {
        return Objects.hash(startX, startY, targetX);
    }

    @Override
    public String toString() {
        return "EnPassant{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", targetX=" + targetX +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        ChessGameState gs = (ChessGameState) gameState;
        int direction = (gs.getPiece(startX, startY).getOwnerId() == 0) ? 1 : -1; // Determine the direction based on the owner ID
        ChessPiece startPiece = gs.getPiece(startX, startY);
        ChessPiece targetPiece = gs.getPiece(targetX, startY);
        String startPieceName = startPiece != null ? startPiece.getChessPieceType().toString() : "empty";
        String targetPieceName = targetPiece != null ? targetPiece.getChessPieceType().toString() : "empty";
        String startSquare = gs.getChessCoordinates(startX, startY);
        String targetSquare = gs.getChessCoordinates(targetX, startY+direction); // The target Y position for the en passant move
        String output = "En Passant: " + startPieceName + " at " + startSquare + " captures " + targetPieceName + " at " + targetSquare;
        return output;
    }
}
