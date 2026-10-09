package games.hareandtortoise.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.HareAndTortoiseParameters;

import java.util.Objects;

import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

public class Move extends AbstractAction {

    public final int from;
    public final int to;

    public Move(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gs;
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        int paid = to > from ? params.moveCost(to - from) : 0;
        if (to > from)
            state.moveRunner(player, to, -paid);
        else
            state.moveRunner(player, to, params.carrotsPerTortoiseStep * (from - to));
        BOARD[to].landOn(state, player, paid);
        return true;
    }

    @Override
    public Move copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Move other && other.from == from && other.to == to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to) + 480113;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) gameState.getGameParameters();
        if (to < from)
            return "Back to tortoise square " + to + " (draw " + params.carrotsPerTortoiseStep * (from - to) + ")";
        String target = to == HOME_SQUARE ? "HOME" : to + " (" + BOARD[to].name().toLowerCase() + ")";
        return "Move to " + target + " (pay " + params.moveCost(to - from) + ")";
    }

    @Override
    public String toString() {
        return "Move " + from + " -> " + to;
    }
}
