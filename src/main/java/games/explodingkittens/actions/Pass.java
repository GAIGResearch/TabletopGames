package games.explodingkittens.actions;

import core.actions.DoNothing;

public class Pass extends DoNothing {
    @Override
    public String toString() {
        return "Player passes";
    }

    @Override
    public Pass copy() {
        return this;
    }
}
