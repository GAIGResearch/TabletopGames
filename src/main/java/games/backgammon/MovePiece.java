package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.actions.Move;

public class MovePiece extends AbstractAction {

    public final int from;
    public final int to;

    /**
     * Move Piece from one point to another
     */
    public MovePiece(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BGGameState bgp = (BGGameState) gs;
        int playerId = bgp.getCurrentPlayer();
        int piecesAtStart = bgp.getPiecesOnPoint(playerId, from);
        if (piecesAtStart > 0) {
            if (to > -1) {
                // we are not bearing off
                // check to see if opponent has pieces on the point
                int opponentPieces = bgp.getPiecesOnPoint(1 - playerId, to);
                if (opponentPieces > 1) {
                    throw new IllegalArgumentException("Cannot move to a point occupied by two or more opponent pieces");
                } else if (opponentPieces == 1) {
                    // hit the opponent's piece
                    bgp.movePieceToBar(1 - playerId, to); // move to bar
                }
            }
            bgp.movePiece(playerId, from, to);
        } else {
            throw new IllegalArgumentException("No pieces on the from point");
        }
        // mark the die as used
        int dieValue = calculateDieValueUsed(bgp);
        bgp.useDiceValue(dieValue);
        return true;
    }

    protected int calculateDieValueUsed(BGGameState bgp) {
        // from and to are the physical points. To find the die value used, we need to consider the
        // distance moved in terms of the game rules (via playerTrack)

        BGParameters params = (BGParameters) bgp.getGameParameters();
        int player = bgp.getCurrentPlayer();
        if (from == 0) {
            // in this case we are moving from the bar
            return bgp.getLogicalPosition(player, to);
        } else if (to == -1) {
            // in this case any die value will do...so we take the lowest available one
            int minDieValue = bgp.playerTrackMapping[0].length - bgp.getLogicalPosition(player, from);
            int min = params.diceSides + 1;
            for (int d : bgp.getAvailableDiceValues()) {
                if (d < min && d >= minDieValue) {
                    min = d;
                }
            }
            if (min == params.diceSides + 1)
                throw new IllegalArgumentException("No dice available for this move");
            return min;
        } else {
            return bgp.getLogicalPosition(player, to) - bgp.getLogicalPosition(player, from);
        }
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof MovePiece mp) {
            return from == mp.from && to == mp.to;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * from + to;
    }

    @Override
    public String toString() {
        return String.format("Move Piece from %d to %d", from, to);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        BGGameState bgp = (BGGameState) gameState;
        StringBuilder sb = new StringBuilder();
        int player = bgp.getCurrentPlayer();
        int pieces = bgp.getPiecesOnPoint(player, from);
        sb.append("Move Piece from ").append(from).append(" to ").append(to).append(" (1 of ").append(pieces).append(" pieces)");
        return sb.toString();
    }
}
