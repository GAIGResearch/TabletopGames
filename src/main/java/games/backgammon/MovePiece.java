package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class MovePiece extends AbstractAction {

    public final int from;
    public final int to;
    public final boolean diceOverride;

    /**
     * Move Piece from one point to another
     *
     * @param from 0 for the bar, 1-24 for the points
     * @param to   1-24 for the points, -1 to bear off
     */
    public MovePiece(int from, int to) {
        this(from, to, false);
    }

    public MovePiece(int from, int to, boolean diceOverride) {
        this.from = from;
        this.to = to;
        this.diceOverride = diceOverride;
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
        if (!diceOverride) {
            // mark the die as used
            int dieValue = switch (to) {
                case -1 -> {
                    // in this case any die value will do...so we take the lowest available one
                    int minDieValue = Math.min(from, 25 - from);
                    int min = 7;
                    for (int d : bgp.getAvailableDiceValues()) {
                        if (d < min && d >= minDieValue) {
                            min = d;
                        }
                    }
                    if (min == 7)
                        throw new IllegalArgumentException("No dice available for this move");
                    yield min;
                }
                // TODO: When we introduce XII this will need to change as progression is not always linear
                // will probably be easiest to record the dice used when the action is created
                default -> {
                    int trial = Math.abs(from - to);
                    if (trial <= 6)
                        yield trial;
                    yield 24 - trial + 1; // convert to die value
                }
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
