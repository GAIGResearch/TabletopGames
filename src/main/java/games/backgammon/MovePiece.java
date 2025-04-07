package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class MovePiece extends AbstractAction {

    public final int from;
    public final int to;

    public MovePiece(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BGGameState bgp = (BGGameState) gs;
        int playerId = bgp.getCurrentPlayer();
        if (bgp.getPiecesOnPoint(playerId, from) > 0) {
            // check to see if opponent has pieces on the point
            int opponentPieces = bgp.getPiecesOnPoint(1 - playerId, bgp.getPlayerPieces(0).length - to - 1);
            if (opponentPieces > 1) {
                throw new IllegalArgumentException("Cannot move to a point occupied by two or more opponent pieces");
            } else if (opponentPieces == 1) {
                // hit the opponent's piece
                bgp.movePieceToBar(1 - playerId, bgp.getPlayerPieces(0).length - to - 1); // move to bar
                // then move ours
                bgp.movePiece(playerId, from, to);
            } else {
                // we just move the piece
                bgp.movePiece(playerId, from, to);
            }
        } else {
            throw new IllegalArgumentException("No pieces on the from point");
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MovePiece movePiece = (MovePiece) obj;
        return from == movePiece.from && to == movePiece.to;
    }

    @Override
    public int hashCode() {
        return 31 * from + to - 320;
    }

    @Override
    public String toString() {
        return String.format("Move Piece from %d to %d", from, to);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        BGGameState bgp = (BGGameState) gameState;
        // a slightly more meaningful string that includes the number of pieces on the starting point, whether there was a hit
        // If there was no hit, then report the total number of pieces on the point after the move
        StringBuilder sb = new StringBuilder();
        int player = bgp.getCurrentPlayer();
        sb.append("Move Piece from ").append(from).append(" to ").append(to).append(" (1 of ").append(bgp.getPiecesOnPoint(player, from)).append(" pieces.\n");
        if (bgp.getPiecesOnPoint(1 - player, bgp.getPlayerPieces(0).length - to - 1) > 0) {
            sb.append("Hit opponent's piece on point ").append(bgp.getPlayerPieces(0).length - to - 1).append("\n");
        } else {
            sb.append("No hit, total pieces on point ").append(to).append(" after move: ").append(1 + bgp.getPiecesOnPoint(player, to)).append("\n");
        }
        return sb.toString();
    }
}
