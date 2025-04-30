package games.chess.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.chess.ChessGameState;
import games.chess.components.ChessPiece;
import games.chess.components.ChessPiece.MovedState;

import java.lang.System;
import java.util.Objects;

import org.checkerframework.checker.units.qual.C;

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
public class Castle extends AbstractAction {
    public enum CastleType {
        KING_SIDE,
        QUEEN_SIDE
    }
    final CastleType castleType;

    public Castle(CastleType castleType) {
        this.castleType = castleType;
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
        int[] kingPos = gs.getKingPosition(gs.getCurrentPlayer());
        ChessPiece king = gs.getPiece(kingPos[0], kingPos[1]);
        ChessPiece rook = null;

        if (castleType == CastleType.KING_SIDE) {
            // Move the king and rook to their new positions for king-side castling
            rook = gs.getPiece(kingPos[0] + 3, kingPos[1]);
            gs.updatePiecePosition(king, kingPos[0] + 2, kingPos[1]);
            gs.updatePiecePosition(rook, kingPos[0] + 1, kingPos[1]);
        } else if (castleType == CastleType.QUEEN_SIDE) {
            // Move the king and rook to their new positions for queen-side castling
            rook = gs.getPiece(kingPos[0] - 4, kingPos[1]);
            gs.updatePiecePosition(king, kingPos[0] - 2, kingPos[1]);
            gs.updatePiecePosition(rook, kingPos[0] - 1, kingPos[1]);
        } else {
            // Invalid castling type
            return false;
        }

        // Set the moved flags.
        king.setMoved(MovedState.MOVED); // Set the moved flag for the king
        rook.setMoved(MovedState.MOVED); // Set the moved flag for the roo
           
        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public Castle copy() {
        // immutable        
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: compare all other variables in the class
        return obj instanceof Castle && ((Castle) obj).castleType == this.castleType;
    }

    @Override
    public int hashCode() {
        return castleType.ordinal() + 467272;
    }

    @Override
    public String toString() {
        // TODO: Replace with appropriate string, including any action parameters
        return "Castle{" +
                "castleType=" + castleType +
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
        String output = "Castle: ";
        if (castleType == CastleType.KING_SIDE) {
            output += "King-side castling";
        } else if (castleType == CastleType.QUEEN_SIDE) {
            output += "Queen-side castling";
        } else {
            output += "Invalid castling type";
        }
        output += " for player " + gameState.getCurrentPlayer() + ".";


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
