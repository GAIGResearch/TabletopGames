package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player does not buy the unowned property they have landed on. The Bank auctions it (Auction).
 */
public class DeclineProperty extends AbstractAction {

    public final MonopolySquare square;

    public DeclineProperty(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // the Bank auctions it, starting with the next player
        MonopolyGameState state = (MonopolyGameState) gs;
        state.setActionInProgress(new Auction(state, square, state.nextPlayerIn(state.getCurrentPlayer())));
        return true;
    }

    @Override
    public DeclineProperty copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeclineProperty d && d.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640223;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Do not buy " + square.name();
    }

    @Override
    public String toString() {
        return "Decline " + square.name();
    }
}
