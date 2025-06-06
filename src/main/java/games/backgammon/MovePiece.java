package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class MovePiece extends AbstractAction {

    public final int from;
    public final int to;
    public final boolean diceOverride;

    /**
     * Move Piece from one point to another (-1 is the bar)
     *
     * @param from -1 for the bar, 0-23 for the points
     * @param to   0-23 for the points, -1 to bear off
     */
    public MovePiece(int from, int to) {
        this(from, to, false);
    }

    public MovePiece(int from, int to, boolean diceOverride) {
        this.from = from;
        this.to = to;
        this.diceOverride = diceOverride;
        if (from > -1 && from < to) {
            throw new IllegalArgumentException("from must be greater than to");
        }
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BGGameState bgp = (BGGameState) gs;
        int playerId = bgp.getCurrentPlayer();
        int piecesAtStart = from < 0 ? bgp.getPiecesOnBar(playerId) : bgp.getPiecesOnPoint(playerId, from);
        int boardLength = bgp.getPlayerPieces(0).length;
        if (piecesAtStart > 0) {
            if (to < 0) {
                // we are bearing off
                bgp.movePiece(playerId, from, to);
            } else {
                // check to see if opponent has pieces on the point
                int opponentPieces = bgp.getPiecesOnPoint(1 - playerId, boardLength - to - 1);
                if (opponentPieces > 1) {
                    throw new IllegalArgumentException("Cannot move to a point occupied by two or more opponent pieces");
                } else if (opponentPieces == 1) {
                    // hit the opponent's piece
                    bgp.movePieceToBar(1 - playerId, boardLength - to - 1); // move to bar
                    // then move ours
                    bgp.movePiece(playerId, from, to);
                } else {
                    // we just move the piece
                    bgp.movePiece(playerId, from, to);
                }
            }
        } else {
            throw new IllegalArgumentException("No pieces on the from point");
        }
        if (!diceOverride) {
            // mark the die as used
            int dieValue = switch (to) {
                case -1 -> {
                    // in this case any die value will do...so we take the lowest available one
                    int min = 6;
                    for (int d : bgp.getAvailableDiceValues()) {
                        if (d < min && d >= from + 1) {
                            min = d;
                        }
                    }
                    if (min == 0)
                        throw new IllegalArgumentException("No dice available for this move");
                    yield min;
                }
                default -> from < 0 ? boardLength - to : from - to;
            };
            bgp.useDiceValue(dieValue);
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
        return from == movePiece.from && to == movePiece.to && diceOverride == movePiece.diceOverride;
    }

    @Override
    public int hashCode() {
        return 31 * from + to - 320 * (diceOverride ? 1 : 0);
    }

    @Override
    public String toString() {
        return String.format("Move Piece from %d to %d", from + 1, to + 1);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        BGGameState bgp = (BGGameState) gameState;
        StringBuilder sb = new StringBuilder();
        int player = bgp.getCurrentPlayer();
        int pieces = from < 0 ? bgp.getPiecesOnBar(player) : bgp.getPiecesOnPoint(player, from);
        sb.append("Move Piece from ").append(from + 1).append(" to ").append(to + 1).append(" (1 of ").append(pieces).append(" pieces)");
        return sb.toString();
    }
}
