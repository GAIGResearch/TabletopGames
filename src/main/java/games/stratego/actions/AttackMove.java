package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import utilities.Utils;

public class AttackMove extends Move{

    private final int attackedPieceID;

    public AttackMove(int movedPieceID, int[] destinationCoordinate, int attackedPieceID) {
        super(movedPieceID, destinationCoordinate);
        this.attackedPieceID = attackedPieceID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean movedTileEmptied = true;
        boolean destinationTileEmptied = true;
        boolean destinationTileSet = true;

        Piece movedPiece = (Piece) gs.getComponentById(movedPieceID);
        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();
        Piece attackedPiece = (Piece) gs.getComponentById(attackedPieceID);
        int movedPieceRank = movedPiece.getPieceRank();
        int attackedPieceRank = attackedPiece.getPieceRank();
        movedPiece.changePieceKnownFlag(true);
        attackedPiece.changePieceKnownFlag(true);

        if (attackedPiece.getPieceType() == Piece.PieceType.FLAG){
            gs.setGameStatus(Utils.GameResult.GAME_END);
            gs.setPlayerResult(Utils.GameResult.WIN, gs.getCurrentPlayer());
            gs.setPlayerResult(Utils.GameResult.LOSE, 1-gs.getCurrentPlayer());
        } else if (attackedPiece.getPieceType() == Piece.PieceType.BOMB){
            if (movedPiece.getPieceType() == Piece.PieceType.MINER){
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
                destinationTileEmptied = board.setElement(destinationCoordinate[0],
                        destinationCoordinate[1], null);
                destinationTileSet = board.setElement(destinationCoordinate[0],
                        destinationCoordinate[1], movedPiece);
                movedPiece.setPiecePosition(destinationCoordinate);
            } else{
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
            }
        } else if (attackedPiece.getPieceType() == Piece.PieceType.MARSHAL){
            if (movedPiece.getPieceType() == Piece.PieceType.SPY){
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
                destinationTileEmptied = board.setElement(destinationCoordinate[0],
                        destinationCoordinate[1], null);
                destinationTileSet = board.setElement(destinationCoordinate[0],
                        destinationCoordinate[1], movedPiece);
                movedPiece.setPiecePosition(destinationCoordinate);
            } else{
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
            }
        } else if (movedPieceRank > attackedPieceRank){
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
            destinationTileEmptied = board.setElement(destinationCoordinate[0],
                    destinationCoordinate[1], null);
            destinationTileSet = board.setElement(destinationCoordinate[0],
                    destinationCoordinate[1], movedPiece);
            movedPiece.setPiecePosition(destinationCoordinate);
        } else if (movedPieceRank == attackedPieceRank){
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
            board.setElement(destinationCoordinate[0], destinationCoordinate[1], null);
        } else {
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
        }
        return (movedTileEmptied && destinationTileEmptied && destinationTileSet);
    }

    @Override
    public AttackMove copy() {
        return new AttackMove(movedPieceID, destinationCoordinate.clone(), attackedPieceID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Attack (" + movedPieceID + "; " + attackedPieceID + " [" + destinationCoordinate[0] + "," + destinationCoordinate[1] + "])";
    }

    public int getAttackedPieceID() {
        return attackedPieceID;
    }
}