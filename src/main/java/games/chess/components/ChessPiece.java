package games.chess.components;

import java.util.Objects;

import core.components.Token;


/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class ChessPiece extends Token {

    public enum ChessPieceType {
        KING,
        QUEEN,
        ROOK,
        BISHOP,
        KNIGHT,
        PAWN
    }

    //Type of the piece (e.g. KING, QUEEN, etc.)
    private ChessPieceType type;


    public enum MovedState {
        NOT_MOVED,
        MOVED, 
        NOT_RELEVANT
    }
    
    //Wether the piece has moved or not (e.g. for castling, en passant, etc.), only relevant for pawns, kings and rooks
    private MovedState movedState;

    private int x;
    private int y;
    private boolean enPassant = false; // Whether the piece can be captured en passant (only relevant for pawns)

    //Constructor for the piece, takes the type of the piece and the owner ID (0 for white, 1 for black)
    public ChessPiece(ChessPieceType type, int ownerID, int x, int y) {
        super(type.toString());
        this.type = type;
        this.setOwnerId(ownerID);
        if (type == ChessPieceType.KING || type == ChessPieceType.ROOK || type == ChessPieceType.PAWN) {
            this.movedState = MovedState.NOT_MOVED;
        } else {
            this.movedState = MovedState.NOT_RELEVANT;
        }
        this.x = x;
        this.y = y;
    }

    public ChessPiece(ChessPieceType type, int ownerID, int x, int y, MovedState moved) {
        super(type.toString());
        this.type = type;
        this.setOwnerId(ownerID);
        this.movedState = moved;
        this.x = x;
        this.y = y;
    }

    protected ChessPiece(ChessPieceType type, int ownerID, int x, int y, MovedState moved, int componentID) {
        super(type.toString(), componentID);
        this.type = type;
        this.setOwnerId(ownerID);
        this.movedState = moved;
        this.x = x;
        this.y = y;
    }

    @Override
    public ChessPiece copy() {
        ChessPiece copy = new ChessPiece(type, ownerId, x, y, movedState, componentID);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessPiece)) return false;
        ChessPiece other = (ChessPiece) o;
        return this.type == other.type && this.getOwnerId() == other.getOwnerId() &&
                this.movedState == other.movedState && this.x == other.x && this.y == other.y &&
                this.enPassant == other.enPassant;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ownerId, movedState, enPassant, x, y);
    }

    @Override
    public String toString() {
        //Return first letter of type and color (e.g. "wK" for white king, "bQ" for black queen) lower case the knight
        String color = (getOwnerId() == 0) ? "w" : "b";
        String pieceType = (type == ChessPieceType.KNIGHT) ? "n" : type.toString().substring(0, 1);
        return color + pieceType;
    }

    public ChessPieceType getChessPieceType() {
        return type;
    }
    public void setChessPieceType(ChessPieceType type) {
        this.type = type;
    }
    public MovedState getMoved() {
        return movedState;
    }
    public void setMoved(MovedState movedState) {
        this.movedState = movedState;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int[] getPosition() {
        return new int[] {x, y};
    }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public boolean isEnPassant() {
        return enPassant;
    }

    public void setEnPassant(boolean enPassant) {
        this.enPassant = enPassant;
    }
    
}
