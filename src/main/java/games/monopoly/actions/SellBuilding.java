package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player sells a building from one of their streets (MonopolyGameState.canSellBuilding) back to the Bank
 * for MonopolyParameters.buildingSaleValue: a house, or a hotel, which leaves four houses.
 */
public class SellBuilding extends AbstractAction {

    public final MonopolySquare square;

    public SellBuilding(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        state.setBuildings(square, state.getBuildings(square) - 1);
        state.setCash(player, state.getCash(player) + ((MonopolyParameters) state.getGameParameters()).buildingSaleValue(square));
        return true;
    }

    @Override
    public SellBuilding copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SellBuilding s && s.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640251;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Sell a building on " + square.name();
    }

    @Override
    public String toString() {
        return "Sell " + square.name();
    }
}
