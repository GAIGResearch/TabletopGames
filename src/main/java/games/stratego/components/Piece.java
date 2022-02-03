package games.stratego.components;

import core.components.GridBoard;
import core.components.Token;
import games.stratego.StrategoGameState;
import games.stratego.StrategoParams;
import games.stratego.actions.AttackMove;
import games.stratego.actions.Move;
import games.stratego.actions.NormalMove;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Piece extends Token {

    protected int[] position;
    protected final int rank;
    protected final PieceType pieceType;
    protected final Alliance alliance;
    private boolean pieceKnown;

    private final static int[] MOVE_VECTOR = {-1, 1};

    public Piece(PieceType pieceType, Alliance alliance, int[] position) {
        super(pieceType.name());
        this.rank = pieceType.pieceRank;
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = new int[] {position[0], position[1]};
        this.pieceKnown = false;
    }

    protected Piece(PieceType pieceType, Alliance alliance, int[] position, int ID) {
        super(pieceType.name(), ID);
        this.rank = pieceType.pieceRank;
        this.pieceType = pieceType;
        this.alliance = alliance;
        this.position = new int[] {position[0], position[1]};
        this.pieceKnown = false;
    }

    public int[] getPiecePosition(){
        return new int[] {position[0], position[1]};
    }

    public void setPiecePosition(int[] coordinate){
        this.position = new int[] {coordinate[0], coordinate[1]};
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

    public boolean getPieceKnownFlag(){
        return this.pieceKnown;
    }

    public void changePieceKnownFlag(boolean bool){
        this.pieceKnown = bool;
    }

    public Collection<Move> calculateMoves(StrategoGameState gs) {

        GridBoard<Piece> board = gs.getGridBoard();
        StrategoParams params = (StrategoParams) gs.getGameParameters();

        final List<Move> moves = new ArrayList<>();

        if (this.pieceType == PieceType.BOMB || this.pieceType == PieceType.FLAG){
            return moves;
        }

        if (this.pieceType == PieceType.SCOUT){
            for (final int move : MOVE_VECTOR){
                for (int i =0; i<=params.moveSpeed; i++){
                    int[] currentPos = {position[0], position[1]};

                    while(params.isTileValid(currentPos[0],currentPos[1])){
                        currentPos[i] += move;

                        if(params.isTileValid(currentPos[0],currentPos[1])){
                            final Piece pieceAtTile = board.getElement(currentPos[0],currentPos[1]);
                            if (pieceAtTile == null){
                                moves.add(new NormalMove(getComponentID(), currentPos));
                            } else{
                                final Alliance pieceAtTileAlliance = pieceAtTile.getPieceAlliance();
                                if (this.alliance != pieceAtTileAlliance){
                                    moves.add(new AttackMove(getComponentID(), currentPos, pieceAtTile.getComponentID()));
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } else{
            for(final int move : MOVE_VECTOR){
                for (int i=0;i<= params.moveSpeed;i++){
                    int[] currentPos = {position[0], position[1]};
                    currentPos[i] += move;
                    if (params.isTileValid(currentPos[0], currentPos[1])){
                        final Piece pieceAtTile = board.getElement(currentPos[0], currentPos[1]);
                        if (pieceAtTile == null){
                            moves.add(new NormalMove(getComponentID(), currentPos));
                        } else{
                            final Alliance pieceAtTileAlliance = pieceAtTile.getPieceAlliance();
                            if (this.alliance != pieceAtTileAlliance){
                                moves.add(new AttackMove(getComponentID(), currentPos, pieceAtTile.getComponentID()));
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public Piece copy() {
        Piece copy = new Piece(pieceType, alliance, new int[] {position[0], position[1]}, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public Piece partialCopy(PieceType hiddenPieceType){
        Piece copy = new Piece(hiddenPieceType, alliance, new int[] {position[0], position[1]}, componentID);
        copyComponentTo(copy);
        return copy;
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

        public String rankToString(){return String.valueOf(this.pieceRank);}

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
