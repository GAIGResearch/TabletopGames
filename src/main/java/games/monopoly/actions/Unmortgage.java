package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player lifts the mortgage on one of their properties, paying the Bank
 * MonopolyParameters.unmortgageCost.
 */
public class Unmortgage extends AbstractAction {

    public final MonopolySquare square;

    public Unmortgage(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        state.pay(state.getCurrentPlayer(), -1, ((MonopolyParameters) state.getGameParameters()).unmortgageCost(square));
        state.setMortgaged(square, false);
        return true;
    }

    @Override
    public Unmortgage copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Unmortgage u && u.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640243;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Unmortgage " + square.name();
    }

    @Override
    public String toString() {
        return "Unmortgage " + square.name();
    }
}
