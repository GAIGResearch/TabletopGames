package games.chess.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.chess.ChessGameState;
import games.chess.components.ChessPiece;
import java.lang.System;
import java.util.Objects;

/**
<<<<<<< HEAD
    * EnPassant action for chess game. When a pawn moves two squares forward from its starting position, it can be captured by an opponent's pawn as if it had only moved one square. This action captures the opponent's pawn and moves the current player's pawn to the target position.
    * The target position is one square diagonally behind the opponent's pawn. This action is only available immediately after the opponent's pawn moves two squares forward.
=======
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
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
<<<<<<< HEAD

=======
    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
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
