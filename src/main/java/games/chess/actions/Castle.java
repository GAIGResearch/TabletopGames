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
 * Castle action for chess game. This action allows the player to perform castling, a special move involving the king and rook.
 * The king moves two squares towards the rook, and the rook moves to the square next to the king.
 * Castling is only allowed if neither piece has moved before, there are no pieces between them, the king is not in check, and the squares the king moves through are not attacked.
 * Conditions are checked in the forward model.
 */
public class Castle extends AbstractAction {
    public enum CastleType {
        KING_SIDE,
        QUEEN_SIDE
    }
    public final CastleType castleType;

    public Castle(CastleType castleType) {
        this.castleType = castleType;
    }
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

    @Override
    public Castle copy() {
        // immutable        
        return this;
    }

    @Override
    public boolean equals(Object obj) {
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
    
}
