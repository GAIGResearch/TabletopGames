package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import utilities.Utils;

import java.util.Arrays;
import java.util.Objects;

public class AttackMove extends Move {

    private final int attackedPieceID;

    public AttackMove(int movedPieceID, int attackedPieceID) {
        super(movedPieceID);
        this.attackedPieceID = attackedPieceID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean movedTileEmptied = true;
        boolean destinationTileEmptied = true;
        boolean destinationTileSet = true;

        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();
        Piece movedPiece = (Piece) gs.getComponentById(movedPieceID);
        Piece attackedPiece = (Piece) gs.getComponentById(attackedPieceID);
        int movedPieceRank = movedPiece.getPieceRank();
        int attackedPieceRank = attackedPiece.getPieceRank();
        movedPiece.setPieceKnown(true);
        attackedPiece.setPieceKnown(true);

        if (attackedPiece.getPieceType() == Piece.PieceType.FLAG){
            gs.setGameStatus(Utils.GameResult.GAME_END);
            gs.setPlayerResult(Utils.GameResult.WIN, gs.getCurrentPlayer());
            gs.setPlayerResult(Utils.GameResult.LOSE, 1-gs.getCurrentPlayer());

        } else if (attackedPiece.getPieceType() == Piece.PieceType.BOMB){
            if (movedPiece.getPieceType() == Piece.PieceType.MINER){
                // Bomb is lost and miner moves into bomb's location
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
                destinationTileSet = board.setElement(attackedPiece.getPiecePosition()[0],
                        attackedPiece.getPiecePosition()[1], movedPiece);
                movedPiece.setPiecePosition(attackedPiece.getPiecePosition());
            } else{
                // Piece is lost and removed from the board
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                        movedPiece.getPiecePosition()[1], null);
            }

        } else if (attackedPiece.getPieceType() == Piece.PieceType.MARSHAL && movedPiece.getPieceType() == Piece.PieceType.SPY){
            // Spy removes Marshal, but only if spy is the one attacking
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
            destinationTileSet = board.setElement(attackedPiece.getPiecePosition()[0],
                    attackedPiece.getPiecePosition()[1], movedPiece);
            movedPiece.setPiecePosition(attackedPiece.getPiecePosition());

        } else if (movedPieceRank > attackedPieceRank){
            // Higher rank wins
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
            destinationTileSet = board.setElement(attackedPiece.getPiecePosition()[0],
                    attackedPiece.getPiecePosition()[1], movedPiece);
            movedPiece.setPiecePosition(attackedPiece.getPiecePosition());

        } else if (movedPieceRank == attackedPieceRank){
            // Both pieces are lost
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], null);
            destinationTileEmptied = board.setElement(attackedPiece.getPiecePosition()[0],
                    attackedPiece.getPiecePosition()[1], null);
        } else {
            // Higher rank wins
            movedTileEmptied = board.setElement(attackedPiece.getPiecePosition()[0],
                    attackedPiece.getPiecePosition()[1], null);
            destinationTileSet = board.setElement(movedPiece.getPiecePosition()[0],
                    movedPiece.getPiecePosition()[1], attackedPiece);
            attackedPiece.setPiecePosition(movedPiece.getPiecePosition());
        }
        return (movedTileEmptied && destinationTileEmptied && destinationTileSet);
    }

    @Override
    public AttackMove copy() {
        return new AttackMove(movedPieceID, attackedPieceID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Piece movedPiece = (Piece) gameState.getComponentById(movedPieceID);
        Piece attackedPiece = (Piece) gameState.getComponentById(attackedPieceID);
        return "Attack (" + Arrays.toString(movedPiece.getPiecePosition()) + " [" + movedPiece.getPieceRank() + "]" + " -> " +
                Arrays.toString(attackedPiece.getPiecePosition()) + " [" + attackedPiece.getPieceRank() + "])";
    }

    @Override
    public String getPOString(StrategoGameState gameState) {
        Piece movedPiece = (Piece) gameState.getComponentById(movedPieceID);
        Piece attackedPiece = (Piece) gameState.getComponentById(attackedPieceID);
        return "Attack (" + Arrays.toString(movedPiece.getPiecePosition()) + " -> " +
                Arrays.toString(attackedPiece.getPiecePosition()) + "])";
    }

    @Override
    public int[] to(StrategoGameState gs) {
        Piece attackedPiece = (Piece) gs.getComponentById(attackedPieceID);
        return attackedPiece.getPiecePosition();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AttackMove that = (AttackMove) o;
        return attackedPieceID == that.attackedPieceID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackedPieceID);
    }
}