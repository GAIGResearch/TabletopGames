package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player buys the unowned property they have landed on from the Bank, at its printed price.
 */
public class BuyProperty extends AbstractAction {

    public final MonopolySquare square;

    public BuyProperty(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        state.pay(player, -1, square.price());
        state.setOwner(square, player);
        return true;
    }

    @Override
    public BuyProperty copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BuyProperty b && b.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640219;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buy " + square.name() + " for " + square.price();
    }

    @Override
    public String toString() {
        return "Buy " + square.name();
    }
}
