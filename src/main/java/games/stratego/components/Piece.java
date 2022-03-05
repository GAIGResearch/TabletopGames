package games.stratego.components;

import core.components.GridBoard;
import core.components.Token;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.actions.AttackMove;
import games.stratego.actions.Move;
import games.stratego.actions.NormalMove;
import utilities.Distance;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Piece extends Token {

    protected int[] position;
    protected final PieceType pieceType;
    protected final Alliance alliance;
    private boolean pieceKnown;

    private final static int[] MOVE_VECTOR = {-1, 1};

    public Piece(PieceType pieceType, Alliance alliance, int[] position) {
        super(pieceType.name());
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position;
        this.pieceKnown = false;
    }

    protected Piece(PieceType pieceType, Alliance alliance, int[] position, boolean known, int ID) {
        super(pieceType.name(), ID);
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = position;
        this.pieceKnown = known;
    }

    public int[] getPiecePosition(){
        return new int[] {position[0], position[1]};
    }

    public void setPiecePosition(int[] coordinate){
        this.position = coordinate.clone();
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

    public Collection<Move> calculateMoves(StrategoGameState gs) {

        GridBoard<Piece> board = gs.getGridBoard();
        StrategoParams params = (StrategoParams) gs.getGameParameters();

        List<Move> moves = new ArrayList<>();

        if (!getPieceType().isMovable()){
            return moves;
        }

        if (pieceType == PieceType.SCOUT){
            for (int move : MOVE_VECTOR){  // positive or negative
                for (int i =0; i<2; i++){  // horizontal and vertical
                    int[] newPos = position.clone();

                    while(true){
                        newPos[i] += move;

                        if (params.isTileValid(newPos[0],newPos[1])){
                            if (addMove(board, params, moves, newPos)) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        } else {
            for(int move : MOVE_VECTOR){  // positive or negative
                for (int i=0; i<2; i++){  // horizontal or vertical
                    for (int j = 1; j <= params.moveSpeed; j++) {  // according to movement speed
                        int[] newPos = position.clone();
                        newPos[i] += move * j;

                        if (params.isTileValid(newPos[0], newPos[1])) {
                            if (addMove(board, params, moves, newPos)) break;
                        }
                    }
                }
            }
        }
        return moves;
    }

    private boolean addMove(GridBoard<Piece> board, StrategoParams params, List<Move> moves, int[] newPos) {
        Piece pieceAtTile = board.getElement(newPos[0], newPos[1]);
        if (pieceAtTile == null) {
            moves.add(new NormalMove(getComponentID(), newPos));
        } else {
            if (Distance.manhattan_distance(position, newPos) <= params.attackRange &&
                    alliance != pieceAtTile.getPieceAlliance()) {
                moves.add(new AttackMove(getComponentID(), pieceAtTile.getComponentID()));
            }
            // Reached another piece and cannot occupy same square or jump over, finish
            return true;
        }
        return false;
    }

    @Override
    public Piece copy() {
        Piece copy = new Piece(pieceType, alliance, position.clone(), pieceKnown, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public Piece partialCopy(PieceType hiddenPieceType){
        Piece copy = new Piece(hiddenPieceType, alliance, position.clone(), false, componentID);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Piece piece = (Piece) o;
        return pieceKnown == piece.pieceKnown && Arrays.equals(position, piece.position) && pieceType == piece.pieceType && alliance == piece.alliance;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), pieceType, alliance, pieceKnown);
        result = 31 * result + Arrays.hashCode(position);
        return result;
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
