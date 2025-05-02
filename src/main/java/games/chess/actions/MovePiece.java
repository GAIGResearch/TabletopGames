package games.chess.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.chess.ChessGameState;
import games.chess.components.ChessPiece;
import java.lang.System;
import java.util.Objects;

/**
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
 */
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
    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState ags) {
        ChessGameState gs = (ChessGameState) ags;
        ChessPiece piece = gs.getPiece(startX, startY);

        //Delete the piece in the start position
        gs.deletePiece(piece); // Remove the piece from its original position

        // Set the moved flag to true for the piece being moved.
        if (piece.getMoved() == ChessPiece.MovedState.NOT_MOVED) {
            piece.setMoved(ChessPiece.MovedState.MOVED);
        } 

        // Set en passant flag if the piece is a pawn and the target position is two squares forward
        if (piece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN) {
            if (Math.abs(targetY - startY) == 2) {
                piece.setEnPassant(true); // Set en passant flag for the pawn
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

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public MovePiece copy() {
        // TODO: copy non-final variables appropriately
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
        // TODO: return the hash of all other variables in the class
        return Objects.hash(startX, startY, targetX, targetY);
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "MovePiece{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", targetX=" + targetX +
                ", targetY=" + targetY +
                '}';
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
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
    


    /**
     * This next one is optional.
     *
     *  May optionally be implemented if Actions are not fully visible
     *  The only impact this has is in the GUI, to avoid this giving too much information to the human player.
     *
     *  An example is in Resistance or Sushi Go, in which all cards are technically revealed simultaneously,
     *  but the game engine asks for the moves sequentially. In this case, the action should be able to
     *  output something like "Player N plays card", without saying what the card is.
     * @param gameState - game state to be used to generate the string.
     * @param playerId - player to whom the action should be represented.
     * @return
     */
   // @Override
   // public String getString(AbstractGameState gameState, int playerId);
}
