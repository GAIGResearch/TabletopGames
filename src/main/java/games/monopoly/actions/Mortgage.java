package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player mortgages one of their properties (MonopolyGameState.canMortgage), receiving its mortgage value
 * from the Bank.
 */
public class Mortgage extends AbstractAction {

    public final MonopolySquare square;

    public Mortgage(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        state.setMortgaged(square, true);
        state.setCash(player, state.getCash(player) + square.mortgage());
        return true;
    }

    @Override
    public Mortgage copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Mortgage m && m.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640241;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Mortgage " + square.name() + " for " + square.mortgage();
    }

    @Override
    public String toString() {
        return "Mortgage " + square.name();
    }
}
