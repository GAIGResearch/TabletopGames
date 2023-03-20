package games.stratego.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import utilities.Vector2D;

import java.util.Objects;

public class AttackMove extends Move {

    // Independent:
    private final int attackedPieceID;

    // Dependent:
    private final Vector2D attackedPosition;

    public AttackMove(Vector2D piecePosition, Vector2D attackedPosition) {
        super(piecePosition);
        this.attackedPosition = attackedPosition.copy();
        this.attackedPieceID = -1;
    }

    public AttackMove(int movedPieceID, int attackedPieceID) {
        super(movedPieceID);
        this.attackedPieceID = attackedPieceID;
        this.attackedPosition = null;
    }

    private AttackMove(Vector2D piecePosition, int movedPieceID, Vector2D attackedPosition, int attackedPieceID) {
        super(piecePosition, movedPieceID);
        this.attackedPieceID = attackedPieceID;
        this.attackedPosition = attackedPosition != null? attackedPosition.copy() : null;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean movedTileEmptied = true;
        boolean destinationTileEmptied = true;
        boolean destinationTileSet = true;

        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();
        Piece movedPiece = getPiece((StrategoGameState) gs);
        Piece attackedPiece = getAttackedPiece((StrategoGameState) gs);

        int movedPieceRank = movedPiece.getPieceRank();
        int attackedPieceRank = attackedPiece.getPieceRank();
        movedPiece.setPieceKnown(true);
        attackedPiece.setPieceKnown(true);

        if (attackedPiece.getPieceType() == Piece.PieceType.FLAG){
            gs.setGameStatus(CoreConstants.GameResult.GAME_END);
            gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, gs.getCurrentPlayer());
            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 1-gs.getCurrentPlayer());

        } else if (attackedPiece.getPieceType() == Piece.PieceType.BOMB){
            if (movedPiece.getPieceType() == Piece.PieceType.MINER){
                // Bomb is lost and miner moves into bomb's location
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition().getX(),
                        movedPiece.getPiecePosition().getY(), null);
                destinationTileSet = board.setElement(attackedPiece.getPiecePosition().getX(),
                        attackedPiece.getPiecePosition().getY(), movedPiece);
                movedPiece.setPiecePosition(attackedPiece.getPiecePosition());
            } else{
                // Piece is lost and removed from the board
                movedTileEmptied = board.setElement(movedPiece.getPiecePosition().getX(),
                        movedPiece.getPiecePosition().getY(), null);
            }

        } else if (attackedPiece.getPieceType() == Piece.PieceType.MARSHAL && movedPiece.getPieceType() == Piece.PieceType.SPY){
            // Spy removes Marshal, but only if spy is the one attacking
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition().getX(),
                    movedPiece.getPiecePosition().getY(), null);
            destinationTileSet = board.setElement(attackedPiece.getPiecePosition().getX(),
                    attackedPiece.getPiecePosition().getY(), movedPiece);
            movedPiece.setPiecePosition(attackedPiece.getPiecePosition());

        } else if (movedPieceRank > attackedPieceRank){
            // Higher rank wins
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition().getX(),
                    movedPiece.getPiecePosition().getY(), null);
            destinationTileSet = board.setElement(attackedPiece.getPiecePosition().getX(),
                    attackedPiece.getPiecePosition().getY(), movedPiece);
            movedPiece.setPiecePosition(attackedPiece.getPiecePosition());

        } else if (movedPieceRank == attackedPieceRank){
            // Both pieces are lost
            movedTileEmptied = board.setElement(movedPiece.getPiecePosition().getX(),
                    movedPiece.getPiecePosition().getY(), null);
            destinationTileEmptied = board.setElement(attackedPiece.getPiecePosition().getX(),
                    attackedPiece.getPiecePosition().getY(), null);
        } else {
            // Higher rank wins
            movedTileEmptied = board.setElement(attackedPiece.getPiecePosition().getX(),
                    attackedPiece.getPiecePosition().getY(), null);
            destinationTileSet = board.setElement(movedPiece.getPiecePosition().getX(),
                    movedPiece.getPiecePosition().getY(), attackedPiece);
            attackedPiece.setPiecePosition(movedPiece.getPiecePosition());
        }
        return (movedTileEmptied && destinationTileEmptied && destinationTileSet);
    }

    @Override
    public AttackMove copy() {
        return new AttackMove(position, movedPieceID, attackedPosition, attackedPieceID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Piece movedPiece = getPiece((StrategoGameState) gameState);
        Piece attackedPiece = getAttackedPiece((StrategoGameState) gameState);
        return "Attack (" + movedPiece.getPiecePosition().toString() + " [" + movedPiece.getPieceRank() + "]" + " -> " +
                attackedPiece.getPiecePosition().toString() + " [" + attackedPiece.getPieceRank() + "])";
    }

    @Override
    public String getPOString(StrategoGameState gameState) {
        Piece movedPiece = getPiece(gameState);
        Piece attackedPiece = getAttackedPiece(gameState);
        return "Attack (" + movedPiece.getPiecePosition().toString() + " -> " +
                attackedPiece.getPiecePosition().toString() + "])";
    }

    @Override
    public Vector2D to(StrategoGameState gs) {
        if (attackedPosition != null) {
            return attackedPosition;
        } else {
            return ((Piece) gs.getComponentById(attackedPieceID)).getPiecePosition();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttackMove)) return false;
        if (!super.equals(o)) return false;
        AttackMove that = (AttackMove) o;
        return attackedPieceID == that.attackedPieceID && Objects.equals(attackedPosition, that.attackedPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackedPieceID, attackedPosition);
    }

    public Piece getAttackedPiece(StrategoGameState gs) {
        if (attackedPosition != null) {
            return gs.getGridBoard().getElement(attackedPosition.getX(), attackedPosition.getY());
        } else {
            return (Piece) gs.getComponentById(attackedPieceID);
        }
    }
}