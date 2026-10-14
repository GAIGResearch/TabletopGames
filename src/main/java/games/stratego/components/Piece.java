package games.stratego.components;

import core.components.BoardNode;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.actions.AttackMove;
import games.stratego.actions.NormalMove;
import utilities.Vector2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A piece on the board, owned by the player of its alliance. Immutable, as the values of a SparseGrid must be: a piece
 * that moves or is revealed is replaced on the board by a new one with the same componentID.
 */
public class Piece extends BoardNode {

    protected final Vector2D position;
    protected final PieceType pieceType;
    protected final Alliance alliance;
    private final boolean pieceKnown;

    public Piece(PieceType pieceType, Alliance alliance, Vector2D position) {
        super(-1, pieceType.name());
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position.copy();
        this.pieceKnown = false;
        // as in StrategoConstants.playerMapping
        setOwnerId(alliance.ordinal());
    }

    protected Piece(PieceType pieceType, Alliance alliance, Vector2D position, boolean known, int ID) {
        super(-1, pieceType.name(), ID);
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position.copy();
        this.pieceKnown = known;
    }

    public Vector2D getPiecePosition(){
        return position.copy();
    }

    /**
     * This piece at the new position.
     */
    public Piece movedTo(Vector2D newPosition) {
        Piece retValue = new Piece(pieceType, alliance, newPosition, pieceKnown, componentID);
        copyComponentTo(retValue);
        return retValue;
    }

    /**
     * This piece, known to both players.
     */
    public Piece revealed() {
        if (pieceKnown) return this;
        Piece retValue = new Piece(pieceType, alliance, position, true, componentID);
        copyComponentTo(retValue);
        return retValue;
    }

    public int getPieceRank(){
        return this.pieceType.pieceRank;
    }

    public PieceType getPieceType(){
        return this.pieceType;
    }

    public Alliance getPieceAlliance(){
        return this.alliance;
    }

    public boolean isPieceKnown(){
        return this.pieceKnown;
    }

    public List<AbstractAction> calculateMoves(StrategoGameState gs, ActionSpace actionSpace) {

        StrategoParams params = (StrategoParams) gs.getGameParameters();

        List<AbstractAction> moves = new ArrayList<>();

        if (!getPieceType().isMovable()){
            return moves;
        }

        int maxTravel = params.moveSpeed;
        if (pieceType == PieceType.SCOUT) maxTravel = params.gridSize;

        for (Vector2D.Direction dir: Vector2D.Direction.values4()) {
            for (int j = 1; j <= maxTravel; j++) {
                Vector2D dirCustom = dir.vector2D.mult(j);
                Vector2D newPos = position.copy();
                newPos = newPos.add(dirCustom);
                Piece pieceAtTile = gs.getPiece(newPos);
                if (params.isTileValid(newPos.getX(), newPos.getY())  // Must be walkable tile
                        && (pieceAtTile == null // Ok if empty tile, we can move there
                        || pieceAtTile.getPieceAlliance() != alliance)) {  // Ok if enemy piece at tile, we attack
                    addMove(gs, moves, newPos, dirCustom, actionSpace);
                    // a piece moves no further than the first enemy it reaches
                    if (pieceAtTile != null) break;
                } else {
                    // No more valid moves in this direction
                    break;
                }
            }
        }

        return moves;
    }

    private void addMove(StrategoGameState gs, List<AbstractAction> moves, Vector2D newPos,
                            Vector2D dir, ActionSpace actionSpace) {
        Piece pieceAtTile = gs.getPiece(newPos);
        if (pieceAtTile == null) {
            // Just move
            if (actionSpace.context == ActionSpace.Context.Dependent) {
                // Dependent
                moves.add(new NormalMove(position, dir));
            } else {
                // Independent, default
                moves.add(new NormalMove(getComponentID(), newPos));
            }
        } else {
            // Attack
            if (actionSpace.context == ActionSpace.Context.Dependent) {
                // Dependent
                moves.add(new AttackMove(position, newPos));
            } else {
                // Independent, default
                moves.add(new AttackMove(getComponentID(), pieceAtTile.getComponentID()));
            }
        }
    }

    @Override
    public Piece copy() {
        return this;
    }

    /**
     * This piece as a player who does not know its type might guess it to be.
     */
    public Piece partialCopy(PieceType hiddenPieceType){
        Piece copy = new Piece(hiddenPieceType, alliance, position, false, componentID);
        copyComponentTo(copy);
        // copyComponentTo sets the true type's name
        copy.setComponentName(hiddenPieceType.name());
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece)) return false;
        if (!super.equals(o)) return false;
        Piece piece = (Piece) o;
        return pieceKnown == piece.pieceKnown && Objects.equals(position, piece.position) && pieceType == piece.pieceType && alliance == piece.alliance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position, pieceType, alliance, pieceKnown);
    }

    @Override
    public String toString() {
        return pieceType.pieceRank + " " + pieceType.name();
    }

    public enum PieceType {
        SPY(1),
        SCOUT(2),
        MINER(3),
        SERGEANT(4),
        LIEUTENANT(5),
        CAPTAIN(6),
        MAJOR(7),
        COLONEL(8),
        GENERAL(9),
        MARSHAL(10),
        FLAG(0),
        BOMB(0);

        private final int pieceRank;

        PieceType(int pieceRank){
            this.pieceRank = pieceRank;
        }

        public boolean isMovable() {
            return this != FLAG && this != BOMB;
        }


    }

    public enum Alliance{
        RED,
        BLUE;

        public Color getColor() {
            switch(this) {
                case RED: return Color.red;
                case BLUE: return Color.blue;
            }
            return null;
        }
    }
}
