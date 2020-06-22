package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.awt.*;
import java.util.HashMap;

// TODO: allows rolling dice with some effect
public class RollDie extends AbstractAction {
    HashMap<Color, Integer> dice;

    public RollDie(HashMap<Color, Integer> dice) {
        // Maps from color of die to how many of that color should be rolled
        this.dice = dice;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new RollDie(dice);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RollDie;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Roll die";
    }
}
