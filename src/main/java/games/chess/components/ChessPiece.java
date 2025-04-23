package games.chess.components;

import java.util.Objects;

import org.checkerframework.checker.units.qual.C;

import core.CoreConstants;
import core.components.BoardNode;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class ChessPiece extends BoardNode {

    public enum ChessPieceType {
        KING,
        QUEEN,
        ROOK,
        BISHOP,
        KNIGHT,
        PAWN,
        NONE //Used for empty squares, not a piece
    }

    //Type of the piece (e.g. KING, QUEEN, etc.)
    private ChessPieceType type;

    //Has the piece moved? (e.g. for castling or en passant)
    private boolean moved = false; //Might be better to only have this for pieces where it matters (e.g. pawns, rooks, kings)
    


    public ChessPiece(ChessPieceType type, int ownerID, boolean moved) {
        super(-1, "ChessPiece");
        this.type = type;
        this.setOwnerId(ownerID);
        this.moved = moved;

    }

    protected ChessPiece(ChessPieceType type, int ownerID, boolean moved, int componentID) {
        super(-1, "ChessPiece", componentID);
        this.type = type;
        this.setOwnerId(ownerID);
        this.moved = moved;
    }

    @Override
    public ChessPiece copy() {
        ChessPiece copy = new ChessPiece(type, ownerId, moved, componentID);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessPiece)) return false;
        ChessPiece other = (ChessPiece) o;
        return this.type == other.type && this.getOwnerId() == other.getOwnerId() &&
                this.moved == other.moved && this.componentID == other.componentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ownerId, moved, componentID);
    }

    public ChessPieceType getChessPieceType() {
        return type;
    }
    public boolean getMoved() {
        return moved;
    }
}
