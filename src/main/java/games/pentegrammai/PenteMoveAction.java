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
        Token tokenToMove = null;
        for (Token t : state.board.get(from)) {
            if (t.getOwnerId() == playerId) {
                tokenToMove = t;
                break;
            }
        }
        if (tokenToMove == null) {
            throw new IllegalArgumentException("No token belonging to player " + playerId + " at position " + from);
        }
        if (!state.canPlace(to)) {
            throw new IllegalArgumentException("Cannot place token at position " + to + " (occupied and not sacred)");
        }
        state.board.get(from).remove(tokenToMove);
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
