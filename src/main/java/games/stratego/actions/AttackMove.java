package games.stratego.actions;

import core.AbstractGameState;
import core.CoreConstants;
import games.stratego.StrategoForwardModel;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import games.stratego.metrics.StrategoMetrics;
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
        StrategoGameState sgs = (StrategoGameState) gs;
        // the battle reveals both pieces
        Piece movedPiece = getPiece(sgs).revealed();
        Piece attackedPiece = getAttackedPiece(sgs).revealed();
        Vector2D from = movedPiece.getPiecePosition();
        Vector2D target = attackedPiece.getPiecePosition();
        sgs.setPiece(from, movedPiece);
        sgs.setPiece(target, attackedPiece);
        int movedPieceRank = movedPiece.getPieceRank();
        int attackedPieceRank = attackedPiece.getPieceRank();

        if (attackedPiece.getPieceType() == Piece.PieceType.FLAG){
            gs.logEvent(StrategoMetrics.StrategoEvent.EndCondition, StrategoForwardModel.EndCondition.FLAG_CAPTURE.name() + ":" + gs.getCurrentPlayer());
            gs.setGameStatus(CoreConstants.GameResult.GAME_END);
            gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, gs.getCurrentPlayer());
            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 1-gs.getCurrentPlayer());

        } else if (attackedPiece.getPieceType() == Piece.PieceType.BOMB){
            if (movedPiece.getPieceType() == Piece.PieceType.MINER){
                logBattle(gs, movedPiece, attackedPiece, movedPiece.getOwnerId());
                // Bomb is lost and miner moves into bomb's location
                sgs.setPiece(from, null);
                sgs.setPiece(target, movedPiece.movedTo(target));
            } else {
                logBattle(gs, movedPiece, attackedPiece, attackedPiece.getOwnerId());
                // Piece is lost and removed from the board
                sgs.setPiece(from, null);
            }

        } else if (attackedPiece.getPieceType() == Piece.PieceType.MARSHAL && movedPiece.getPieceType() == Piece.PieceType.SPY){
            logBattle(gs, movedPiece, attackedPiece, movedPiece.getOwnerId());
            // Spy removes Marshal, but only if spy is the one attacking
            sgs.setPiece(from, null);
            sgs.setPiece(target, movedPiece.movedTo(target));

        } else if (movedPieceRank > attackedPieceRank){
            logBattle(gs, movedPiece, attackedPiece, movedPiece.getOwnerId());
            // Higher rank wins
            sgs.setPiece(from, null);
            sgs.setPiece(target, movedPiece.movedTo(target));

        } else if (movedPieceRank == attackedPieceRank){
            logBattle(gs, movedPiece, attackedPiece, -1);  // a tie
            // Both pieces are lost
            sgs.setPiece(from, null);
            sgs.setPiece(target, null);
        } else {
            logBattle(gs, movedPiece, attackedPiece, attackedPiece.getOwnerId());
            // Defender wins, does not move, attacker just gets removed from the board
            sgs.setPiece(from, null);
        }
        return true;
    }

    // the winner is a player id, or -1 for a tie
    private void logBattle(AbstractGameState gs, Piece movedPiece, Piece attackedPiece, int winner) {
        gs.logEvent(StrategoMetrics.StrategoEvent.BattleOutcome,
                movedPiece.getOwnerId() + " vs " + attackedPiece.getOwnerId() +
                " : " + movedPiece.getPieceType().name() + " vs " + attackedPiece.getPieceType().name() + ":" +
                winner);
    }

    @Override
    public AttackMove copy() {
        return new AttackMove(position, movedPieceID, attackedPosition, attackedPieceID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Piece movedPiece = getPiece((StrategoGameState) gameState);
        Piece attackedPiece = getAttackedPiece((StrategoGameState) gameState);
        if (attackedPosition != null) {
            return "Attack " + position.toString() + " [" + movedPiece.getPieceRank() + "]" + " -> " +
                    attackedPosition + " [" + attackedPiece.getPieceRank() + "]";
        }
        return "Attack (" + movedPieceID + " [" + movedPiece.getPieceRank() + "]" + " -> " +
                attackedPieceID + " [" + attackedPiece.getPieceRank() + "])";
    }

    @Override
    public String toString() {
        if (attackedPosition != null) {
            return "Attack " + position.toString() + " -> " + attackedPosition;
        }
        return "Attack " + movedPieceID + " -> " + attackedPieceID;
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
            return gs.getPiece(attackedPieceID).getPiecePosition();
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
            return gs.getPiece(attackedPosition);
        } else {
            return gs.getPiece(attackedPieceID);
        }
    }
}