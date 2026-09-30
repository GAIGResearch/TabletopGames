package games.hareandtortoise.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.HareAndTortoiseParameters;
import games.hareandtortoise.SquareType;

import java.util.Objects;

import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * Moves the current player's runner from one square to another: forwards (paying the Race Card cost), to HOME
 * (HareAndTortoiseParameters.HOME_SQUARE), or back to a tortoise square (drawing carrots).
 */
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
        state.moveRunner(player, to, -params.moveCost(to - from));
        if (to == HOME_SQUARE)
            state.setFinishPosition(player, state.getNPlayersHome() + 1);
        else if (BOARD[to] == SquareType.LETTUCE)
            state.setLettuceToChew(player, true);
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
        return toString();
    }

    @Override
    public String toString() {
        return "Move " + from + " -> " + to;
    }
}
