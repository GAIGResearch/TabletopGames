package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player pays the square's flat tax to the Bank (IncomeTaxChoice).
 */
public class PayFlatTax extends AbstractAction {

    public final MonopolySquare square;

    public PayFlatTax(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        state.pay(state.getCurrentPlayer(), -1, square.tax());
        return true;
    }

    @Override
    public PayFlatTax copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PayFlatTax p && p.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640253;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay " + square.tax() + " " + square.name();
    }

    @Override
    public String toString() {
        return "PayFlatTax " + square.name();
    }
}
