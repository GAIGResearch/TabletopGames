package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.Objects;

/**
 * The current player buys a building for one of their streets (MonopolyGameState.canBuild) at its house cost: a
 * house, or a hotel in place of four houses.
 */
public class BuildHouse extends AbstractAction {

    public final MonopolySquare square;

    public BuildHouse(MonopolySquare square) {
        this.square = square;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        state.pay(state.getCurrentPlayer(), -1, square.houseCost());
        state.setBuildings(square, state.getBuildings(square) + 1);
        return true;
    }

    @Override
    public BuildHouse copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BuildHouse b && b.square.equals(square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(square) + 640249;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Build on " + square.name() + " for " + square.houseCost();
    }

    @Override
    public String toString() {
        return "Build " + square.name();
    }
}
