package games.stratego.components;

import core.components.BoardNode;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.actions.AttackMove;
import games.stratego.actions.NormalMove;
import utilities.Vector2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Piece extends BoardNode {

    protected Vector2D position;
    protected final PieceType pieceType;
    protected final Alliance alliance;
    private boolean pieceKnown;

    public Piece(PieceType pieceType, Alliance alliance, Vector2D position) {
        super(-1, pieceType.name());
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position;
        this.pieceKnown = false;
    }

    protected Piece(PieceType pieceType, Alliance alliance, Vector2D position, boolean known, int ID) {
        super(-1, pieceType.name(), ID);
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position;
        this.pieceKnown = known;
    }

    public Vector2D getPiecePosition(){
        return position;
    }

    public void setPiecePosition(Vector2D coordinate){
        this.position = coordinate.copy();
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

    public void setPieceKnown(boolean bool){
        this.pieceKnown = bool;
    }

    public List<AbstractAction> calculateMoves(StrategoGameState gs, ActionSpace actionSpace) {

        GridBoard board = gs.getGridBoard();
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
                Piece pieceAtTile = (Piece) board.getElement(newPos.getX(), newPos.getY());
                if (params.isTileValid(newPos.getX(), newPos.getY())  // Must be walkable tile
                        && (pieceAtTile == null // Ok if empty tile, we can move there
                        || pieceAtTile.getPieceAlliance() != alliance)) {  // Ok if enemy piece at tile, we attack
                    addMove(board, moves, newPos, dirCustom, actionSpace);
                } else {
                    // No more valid moves in this direction
                    break;
                }
            }
        }

        return moves;
    }

    private void addMove(GridBoard board, List<AbstractAction> moves, Vector2D newPos,
                            Vector2D dir, ActionSpace actionSpace) {
        Piece pieceAtTile = (Piece) board.getElement(newPos.getX(), newPos.getY());
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
        Piece copy = new Piece(pieceType, alliance, position.copy(), pieceKnown, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public Piece partialCopy(PieceType hiddenPieceType){
        Piece copy = new Piece(hiddenPieceType, alliance, position.copy(), false, componentID);
        copyComponentTo(copy);
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
