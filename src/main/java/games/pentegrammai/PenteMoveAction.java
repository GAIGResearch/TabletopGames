package games.pentegrammai;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Token;

public class PenteMoveAction extends AbstractAction {
    final int from, to;

    public PenteMoveAction(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PenteGameState state = (PenteGameState) gameState;
        int playerId = state.getCurrentPlayer();

        // Find a token belonging to the current player at 'from'
        Token tokenToMove;
        if (from == -1) {
            tokenToMove = state.offBoard.stream().filter(t -> t.getOwnerId() == playerId)
                    .findFirst()
                    .orElse(null);
        } else {
            tokenToMove = state.board.get(from).stream()
                    .filter(t -> t.getOwnerId() == playerId)
                    .findFirst()
                    .orElse(null);
        }
        if (tokenToMove == null) {
            throw new IllegalArgumentException("No token belonging to player " + playerId + " at position " + from);
        }
        if (!state.canPlace(to)) {
            throw new IllegalArgumentException("Cannot place token at position " + to + " (occupied and not sacred)");
        }
        if (from == -1)
            state.offBoard.remove(tokenToMove);
        else
            state.board.get(from).remove(tokenToMove);

        // Now check for blot
        if (state.getParams().kiddsVariant) {
            // In Kidd's variant, we can capture if the target is occupied by exactly one of the opponent's pieces
            if (state.getPiecesAt(to, 1 - playerId) == 1) {
                if (state.getPiecesAt(to, playerId) != 0) {
                    throw new AssertionError("Both players cannot have pieces on the same point in Kidd's variant");
                }
                Token removed = state.board.get(to).remove(0); // Remove the opponent's piece
                state.setOffBoard(removed);
                state.blotCount[playerId]++;
            }
        }

        state.board.get(to).add(tokenToMove);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable action, no need to copy
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PenteMoveAction other)) return false;
        return from == other.from && to == other.to;
    }

    @Override
    public int hashCode() {
        return 31 * from + to;
    }

    @Override
    public String toString() {
        return String.format("Move from %d to %d", from, to);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
